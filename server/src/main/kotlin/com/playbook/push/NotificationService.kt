package com.playbook.push

import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.NotificationDedupTable
import com.playbook.db.tables.NotificationSettingsTable
import com.playbook.db.tables.NotificationsTable
import com.playbook.db.tables.PushTokensTable
import com.playbook.db.tables.TeamMembershipsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class NotificationService(
    private val pushService: PushService,
) {

    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * NT-025: Resolve user IDs to notify for a given event and notification type.
     * Queries team members for the event, filters by their notification_settings preference.
     * Only members who have push tokens (active) are included.
     */
    suspend fun resolveRecipients(eventId: String, type: String): List<String> =
        newSuspendedTransaction {
            val eid = UUID.fromString(eventId)

            val teamIds = EventTeamsTable.selectAll()
                .where { EventTeamsTable.eventId eq eid }
                .map { it[EventTeamsTable.teamId] }

            if (teamIds.isEmpty()) return@newSuspendedTransaction emptyList()

            val memberIds = TeamMembershipsTable.selectAll()
                .where { TeamMembershipsTable.teamId inList teamIds }
                .map { it[TeamMembershipsTable.userId] }
                .distinct()

            if (memberIds.isEmpty()) return@newSuspendedTransaction emptyList()

            // Get users who have at least one push token (active members)
            val usersWithTokens = PushTokensTable.selectAll()
                .where { PushTokensTable.userId inList memberIds }
                .map { it[PushTokensTable.userId] }
                .distinct()
                .toSet()

            // Filter by notification settings for this type
            val settingsByUser = NotificationSettingsTable.selectAll()
                .where { NotificationSettingsTable.userId inList usersWithTokens.toList() }
                .associate { row ->
                    row[NotificationSettingsTable.userId] to row
                }

            usersWithTokens.filter { uid ->
                val settings = settingsByUser[uid]
                // If no settings row, defaults are all true — include
                if (settings == null) return@filter true
                when (type) {
                    "new_event" -> settings[NotificationSettingsTable.newEvents]
                    "event_changed" -> settings[NotificationSettingsTable.eventChanges]
                    "event_cancelled" -> settings[NotificationSettingsTable.eventCancellations]
                    "reminder" -> settings[NotificationSettingsTable.reminders]
                    "attendance_response" -> settings[NotificationSettingsTable.attendancePerResponse]
                    "attendance_summary" -> settings[NotificationSettingsTable.attendanceSummary]
                    "abwesenheit_changed" -> settings[NotificationSettingsTable.abwesenheitChanges]
                    else -> true
                }
            }.map { it.toString() }
        }

    /**
     * NT-026: Dedup check. Returns true if already sent (key exists), else inserts key.
     */
    suspend fun dedup(
        userId: String,
        type: String,
        refId: String?,
        timeBucket: String,
    ): Boolean = newSuspendedTransaction {
        val raw = "$userId:$type:${refId ?: ""}:$timeBucket"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val existing = NotificationDedupTable.selectAll()
            .where { NotificationDedupTable.key eq hash }
            .singleOrNull()

        if (existing != null) return@newSuspendedTransaction true

        NotificationDedupTable.insertIgnore {
            it[key] = hash
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        false
    }

    /**
     * NT-027: Ensure a notification_settings row exists for a user (INSERT ... ON CONFLICT DO NOTHING).
     */
    suspend fun ensureSettings(userId: String) = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        NotificationSettingsTable.insertIgnore {
            it[NotificationSettingsTable.userId] = uid
        }
    }

    /**
     * Insert a notification row and send a push notification to the given user.
     */
    suspend fun sendToUser(
        userId: String,
        type: String,
        title: String,
        body: String,
        deepLink: String,
        referenceId: String? = null,
    ) {
        insertNotification(userId, type, title, body, deepLink, referenceId)
        runCatching {
            pushService.send(listOf(userId), title, body, deepLink, mapOf("type" to type))
        }.onFailure { ex ->
            log.error("Push send failed for user $userId type=$type", ex)
        }
    }

    /**
     * Insert notification rows and send push to multiple users.
     */
    suspend fun sendToUsers(
        userIds: List<String>,
        type: String,
        title: String,
        body: String,
        deepLink: String,
        referenceId: String? = null,
    ) {
        userIds.forEach { uid ->
            insertNotification(uid, type, title, body, deepLink, referenceId)
        }
        if (userIds.isNotEmpty()) {
            runCatching {
                pushService.send(userIds, title, body, deepLink, mapOf("type" to type))
            }.onFailure { ex ->
                log.error("Push send failed for ${userIds.size} users type=$type", ex)
            }
        }
    }

    suspend fun insertNotification(
        userId: String,
        type: String,
        title: String,
        body: String,
        deepLink: String,
        referenceId: String? = null,
    ) = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        NotificationsTable.insert {
            it[NotificationsTable.userId] = uid
            it[NotificationsTable.type] = type
            it[NotificationsTable.title] = title
            it[NotificationsTable.body] = body
            it[NotificationsTable.deepLink] = deepLink
            it[NotificationsTable.referenceId] = referenceId?.let { id -> UUID.fromString(id) }
            it[read] = false
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }
}
