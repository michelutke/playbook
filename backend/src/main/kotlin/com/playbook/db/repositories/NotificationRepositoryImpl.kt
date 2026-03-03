package com.playbook.db.repositories

import com.playbook.db.tables.NotificationSettingsTable
import com.playbook.db.tables.NotificationsTable
import com.playbook.db.tables.PushTokensTable
import com.playbook.domain.Notification
import com.playbook.domain.NotificationSettings
import com.playbook.domain.PagedNotifications
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class NotificationRepositoryImpl {

    suspend fun getNotifications(userId: String, page: Int, limit: Int): PagedNotifications =
        newSuspendedTransaction {
            val uid = UUID.fromString(userId)
            val offset = (page * limit).toLong()

            val total = NotificationsTable.selectAll()
                .where { NotificationsTable.userId eq uid }
                .count()
                .toInt()

            val items = NotificationsTable.selectAll()
                .where { NotificationsTable.userId eq uid }
                .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
                .limit(limit, offset)
                .map { row ->
                    Notification(
                        id = row[NotificationsTable.id].toString(),
                        userId = row[NotificationsTable.userId].toString(),
                        type = row[NotificationsTable.type],
                        title = row[NotificationsTable.title],
                        body = row[NotificationsTable.body],
                        deepLink = row[NotificationsTable.deepLink],
                        referenceId = row[NotificationsTable.referenceId]?.toString(),
                        read = row[NotificationsTable.read],
                        createdAt = row[NotificationsTable.createdAt].toInstant()
                            .let { DateTimeFormatter.ISO_INSTANT.format(it) },
                    )
                }

            PagedNotifications(items = items, page = page, total = total)
        }

    suspend fun markRead(notificationId: String, userId: String): Unit = newSuspendedTransaction {
        val nid = UUID.fromString(notificationId)
        val uid = UUID.fromString(userId)
        NotificationsTable.update({
            (NotificationsTable.id eq nid) and (NotificationsTable.userId eq uid)
        }) {
            it[read] = true
        }
    }

    suspend fun markAllRead(userId: String): Unit = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        NotificationsTable.update({ NotificationsTable.userId eq uid }) {
            it[read] = true
        }
    }

    suspend fun deleteNotification(notificationId: String, userId: String): Unit =
        newSuspendedTransaction {
            val nid = UUID.fromString(notificationId)
            val uid = UUID.fromString(userId)
            NotificationsTable.deleteWhere {
                (id eq nid) and (NotificationsTable.userId eq uid)
            }
        }

    suspend fun getSettings(userId: String): NotificationSettings = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        // Ensure settings row exists
        NotificationSettingsTable.insertIgnore {
            it[NotificationSettingsTable.userId] = uid
        }
        val row = NotificationSettingsTable.selectAll()
            .where { NotificationSettingsTable.userId eq uid }
            .single()

        NotificationSettings(
            userId = userId,
            newEvents = row[NotificationSettingsTable.newEvents],
            eventChanges = row[NotificationSettingsTable.eventChanges],
            eventCancellations = row[NotificationSettingsTable.eventCancellations],
            reminders = row[NotificationSettingsTable.reminders],
            reminderLeadTime = row[NotificationSettingsTable.reminderLeadTime],
            attendancePerResponse = row[NotificationSettingsTable.attendancePerResponse],
            attendanceSummary = row[NotificationSettingsTable.attendanceSummary],
            attendanceSummaryLeadTime = row[NotificationSettingsTable.attendanceSummaryLeadTime],
            abwesenheitChanges = row[NotificationSettingsTable.abwesenheitChanges],
        )
    }

    suspend fun upsertSettings(userId: String, update: com.playbook.domain.UpdateNotificationSettingsRequest): NotificationSettings =
        newSuspendedTransaction {
            val uid = UUID.fromString(userId)

            // Ensure row exists with defaults
            NotificationSettingsTable.insertIgnore {
                it[NotificationSettingsTable.userId] = uid
            }

            val current = NotificationSettingsTable.selectAll()
                .where { NotificationSettingsTable.userId eq uid }
                .single()

            NotificationSettingsTable.update({ NotificationSettingsTable.userId eq uid }) {
                it[newEvents] = update.newEvents ?: current[NotificationSettingsTable.newEvents]
                it[eventChanges] = update.eventChanges ?: current[NotificationSettingsTable.eventChanges]
                it[eventCancellations] = update.eventCancellations ?: current[NotificationSettingsTable.eventCancellations]
                it[reminders] = update.reminders ?: current[NotificationSettingsTable.reminders]
                it[reminderLeadTime] = update.reminderLeadTime ?: current[NotificationSettingsTable.reminderLeadTime]
                it[attendancePerResponse] = update.attendancePerResponse ?: current[NotificationSettingsTable.attendancePerResponse]
                it[attendanceSummary] = update.attendanceSummary ?: current[NotificationSettingsTable.attendanceSummary]
                it[attendanceSummaryLeadTime] = update.attendanceSummaryLeadTime ?: current[NotificationSettingsTable.attendanceSummaryLeadTime]
                it[abwesenheitChanges] = update.abwesenheitChanges ?: current[NotificationSettingsTable.abwesenheitChanges]
            }

            val updated = NotificationSettingsTable.selectAll()
                .where { NotificationSettingsTable.userId eq uid }
                .single()

            NotificationSettings(
                userId = userId,
                newEvents = updated[NotificationSettingsTable.newEvents],
                eventChanges = updated[NotificationSettingsTable.eventChanges],
                eventCancellations = updated[NotificationSettingsTable.eventCancellations],
                reminders = updated[NotificationSettingsTable.reminders],
                reminderLeadTime = updated[NotificationSettingsTable.reminderLeadTime],
                attendancePerResponse = updated[NotificationSettingsTable.attendancePerResponse],
                attendanceSummary = updated[NotificationSettingsTable.attendanceSummary],
                attendanceSummaryLeadTime = updated[NotificationSettingsTable.attendanceSummaryLeadTime],
                abwesenheitChanges = updated[NotificationSettingsTable.abwesenheitChanges],
            )
        }

    suspend fun registerPushToken(userId: String, platform: String, token: String): Unit =
        newSuspendedTransaction {
            val uid = UUID.fromString(userId)
            val now = OffsetDateTime.now(ZoneOffset.UTC)

            // Upsert: insert or update updatedAt if (user_id, token) already exists
            val existing = PushTokensTable.selectAll()
                .where { (PushTokensTable.userId eq uid) and (PushTokensTable.token eq token) }
                .singleOrNull()

            if (existing == null) {
                PushTokensTable.insert {
                    it[PushTokensTable.userId] = uid
                    it[PushTokensTable.platform] = platform
                    it[PushTokensTable.token] = token
                    it[updatedAt] = now
                }
            } else {
                PushTokensTable.update({
                    (PushTokensTable.userId eq uid) and (PushTokensTable.token eq token)
                }) {
                    it[PushTokensTable.platform] = platform
                    it[updatedAt] = now
                }
            }
        }

    suspend fun deregisterPushToken(userId: String, token: String): Unit = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        PushTokensTable.deleteWhere {
            (PushTokensTable.userId eq uid) and (PushTokensTable.token eq token)
        }
    }
}
