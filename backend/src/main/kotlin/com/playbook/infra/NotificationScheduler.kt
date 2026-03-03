package com.playbook.infra

import com.playbook.db.tables.AttendanceResponsesTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.NotificationSettingsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.push.NotificationService
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("NotificationScheduler")

/**
 * NT-042: Scheduler that processes reminder and attendance summary notifications every 5 minutes.
 */
fun Application.startNotificationScheduler(notificationService: NotificationService) {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        while (isActive) {
            runCatching { processReminders(notificationService) }
                .onFailure { log.error("Reminder job failed", it) }
            runCatching { processAttendanceSummaries(notificationService) }
                .onFailure { log.error("Summary job failed", it) }
            delay(TimeUnit.MINUTES.toMillis(5))
        }
    }
}

/**
 * NT-043: Find events whose start_at - lead_time falls within the current 5-minute window,
 * for each member with reminders=true. Dedup to avoid re-sending.
 */
private suspend fun processReminders(notificationService: NotificationService) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val windowEnd = now.plusMinutes(5)

    // Max lead time is 7 days (168h). Query events starting within next 7 days.
    val maxLookAhead = now.plusDays(7)

    val upcomingEvents = newSuspendedTransaction {
        EventsTable.selectAll().where {
            (EventsTable.status eq "active") and
            (EventsTable.startAt greaterEq now) and
            (EventsTable.startAt lessEq maxLookAhead)
        }.map { row ->
            Triple(
                row[EventsTable.id].toString(),
                row[EventsTable.title],
                row[EventsTable.startAt].toInstant().epochSecond,
            )
        }
    }

    if (upcomingEvents.isEmpty()) return

    for ((eventId, eventTitle, startEpoch) in upcomingEvents) {
        val eid = java.util.UUID.fromString(eventId)

        // Get team member IDs for this event
        val teamIds = newSuspendedTransaction {
            EventTeamsTable.selectAll()
                .where { EventTeamsTable.eventId eq eid }
                .map { it[EventTeamsTable.teamId] }
        }
        if (teamIds.isEmpty()) continue

        val memberIds = newSuspendedTransaction {
            TeamMembershipsTable.selectAll()
                .where { TeamMembershipsTable.teamId inList teamIds }
                .map { it[TeamMembershipsTable.userId].toString() }
                .distinct()
        }
        if (memberIds.isEmpty()) continue

        // Get settings for members who have reminders=true
        val settingsRows = newSuspendedTransaction {
            NotificationSettingsTable.selectAll()
                .where {
                    (NotificationSettingsTable.userId inList memberIds.map { java.util.UUID.fromString(it) }) and
                    (NotificationSettingsTable.reminders eq true)
                }
                .associate {
                    it[NotificationSettingsTable.userId].toString() to
                    it[NotificationSettingsTable.reminderLeadTime]
                }
        }

        // For members without settings (defaults: reminders=true, lead=1d)
        val membersWithDefaultSettings = memberIds.filter { uid ->
            uid !in settingsRows.keys
        }.associateWith { "1d" }

        val allMembersWithSettings = settingsRows + membersWithDefaultSettings

        // For each member, check if start_at - lead_time falls in [now, now+5min)
        allMembersWithSettings.forEach { (uid, leadTime) ->
            val leadSeconds = parseLeadTimeSeconds(leadTime)
            val fireEpoch = startEpoch - leadSeconds
            val fireTime = OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(fireEpoch),
                ZoneOffset.UTC
            )

            if (!fireTime.isBefore(now) && fireTime.isBefore(windowEnd)) {
                val timeBucket = (fireEpoch / 300).toString()
                val alreadySent = notificationService.dedup(uid, "reminder", eventId, timeBucket)
                if (!alreadySent) {
                    notificationService.sendToUser(
                        userId = uid,
                        type = "reminder",
                        title = "Reminder: $eventTitle",
                        body = "Event starts soon. Don't forget!",
                        deepLink = "playbook://events/$eventId",
                        referenceId = eventId,
                    )
                }
            }
        }
    }
}

/**
 * NT-043: For coaches with attendance_summary=true, fires at start_at - attendance_summary_lead_time.
 * Counts no-response players and sends a summary push.
 */
private suspend fun processAttendanceSummaries(notificationService: NotificationService) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val windowEnd = now.plusMinutes(5)
    val maxLookAhead = now.plusDays(1)

    val upcomingEvents = newSuspendedTransaction {
        EventsTable.selectAll().where {
            (EventsTable.status eq "active") and
            (EventsTable.startAt greaterEq now) and
            (EventsTable.startAt lessEq maxLookAhead)
        }.map { row ->
            Triple(
                row[EventsTable.id].toString(),
                row[EventsTable.title],
                row[EventsTable.startAt].toInstant().epochSecond,
            )
        }
    }

    if (upcomingEvents.isEmpty()) return

    for ((eventId, eventTitle, startEpoch) in upcomingEvents) {
        val eid = java.util.UUID.fromString(eventId)

        val teamIds = newSuspendedTransaction {
            EventTeamsTable.selectAll()
                .where { EventTeamsTable.eventId eq eid }
                .map { it[EventTeamsTable.teamId] }
        }
        if (teamIds.isEmpty()) continue

        // Get coaches for these teams
        val coachIds = newSuspendedTransaction {
            TeamMembershipsTable.selectAll()
                .where {
                    (TeamMembershipsTable.teamId inList teamIds) and
                    (TeamMembershipsTable.role eq "coach")
                }
                .map { it[TeamMembershipsTable.userId].toString() }
                .distinct()
        }
        if (coachIds.isEmpty()) continue

        // Get coaches with attendance_summary=true
        val coachesWithSummary = newSuspendedTransaction {
            NotificationSettingsTable.selectAll()
                .where {
                    (NotificationSettingsTable.userId inList coachIds.map { java.util.UUID.fromString(it) }) and
                    (NotificationSettingsTable.attendanceSummary eq true)
                }
                .associate {
                    it[NotificationSettingsTable.userId].toString() to
                    it[NotificationSettingsTable.attendanceSummaryLeadTime]
                }
        }

        coachesWithSummary.forEach { (uid, leadTime) ->
            val leadSeconds = parseLeadTimeSeconds(leadTime)
            val fireEpoch = startEpoch - leadSeconds
            val fireTime = OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(fireEpoch),
                ZoneOffset.UTC
            )

            if (!fireTime.isBefore(now) && fireTime.isBefore(windowEnd)) {
                val timeBucket = (fireEpoch / 300).toString()
                val alreadySent = notificationService.dedup(uid, "attendance_summary", eventId, timeBucket)
                if (!alreadySent) {
                    // Count no-response players
                    val memberIds = newSuspendedTransaction {
                        TeamMembershipsTable.selectAll()
                            .where {
                                (TeamMembershipsTable.teamId inList teamIds) and
                                (TeamMembershipsTable.role eq "player")
                            }
                            .map { it[TeamMembershipsTable.userId] }
                            .distinct()
                    }

                    val respondedIds = newSuspendedTransaction {
                        AttendanceResponsesTable.selectAll()
                            .where {
                                (AttendanceResponsesTable.eventId eq eid) and
                                (AttendanceResponsesTable.status neq "no-response")
                            }
                            .map { it[AttendanceResponsesTable.userId] }
                            .toSet()
                    }

                    val noResponseCount = memberIds.count { it !in respondedIds }

                    notificationService.sendToUser(
                        userId = uid,
                        type = "attendance_summary",
                        title = "Attendance summary: $eventTitle",
                        body = "$noResponseCount player(s) have not responded yet.",
                        deepLink = "playbook://events/$eventId/attendance",
                        referenceId = eventId,
                    )
                }
            }
        }
    }
}

/**
 * Parse lead time strings like "1d", "2h", "30m" into seconds.
 */
private fun parseLeadTimeSeconds(leadTime: String): Long = when {
    leadTime.endsWith("d") -> leadTime.dropLast(1).toLongOrNull()?.let { it * 86400 } ?: 86400
    leadTime.endsWith("h") -> leadTime.dropLast(1).toLongOrNull()?.let { it * 3600 } ?: 3600
    leadTime.endsWith("m") -> leadTime.dropLast(1).toLongOrNull()?.let { it * 60 } ?: 1800
    else -> 86400 // default 1 day
}
