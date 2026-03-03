package com.playbook.infra

import com.playbook.db.tables.AbwesenheitRulesTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.push.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("NotificationHooks")
private val hookScope = CoroutineScope(Dispatchers.IO)

/**
 * NT-036: Called after event creation. Notifies all team members with new_events=true.
 */
fun launchNotifyEventCreated(eventId: UUID, notificationService: NotificationService) {
    hookScope.launch {
        runCatching { notifyEventCreated(eventId.toString(), notificationService) }
            .onFailure { log.error("notifyEventCreated failed for event=$eventId", it) }
    }
}

/**
 * NT-037: Called after event update if time or location changed.
 */
fun launchNotifyEventUpdated(eventId: UUID, notificationService: NotificationService) {
    hookScope.launch {
        runCatching { notifyEventUpdated(eventId.toString(), notificationService) }
            .onFailure { log.error("notifyEventUpdated failed for event=$eventId", it) }
    }
}

/**
 * NT-038: Called after event cancellation.
 */
fun launchNotifyEventCancelled(eventId: UUID, notificationService: NotificationService) {
    hookScope.launch {
        runCatching { notifyEventCancelled(eventId.toString(), notificationService) }
            .onFailure { log.error("notifyEventCancelled failed for event=$eventId", it) }
    }
}

/**
 * NT-039: Called after attendance response upserted.
 */
fun launchNotifyAttendanceResponse(
    eventId: UUID,
    respondingUserId: UUID,
    status: String,
    notificationService: NotificationService,
) {
    hookScope.launch {
        runCatching {
            notifyAttendanceResponse(
                eventId.toString(),
                respondingUserId.toString(),
                status,
                notificationService,
            )
        }.onFailure { log.error("notifyAttendanceResponse failed for event=$eventId user=$respondingUserId", it) }
    }
}

/**
 * NT-041: Called after abwesenheit rule created or updated.
 */
fun launchNotifyAbwesenheitChange(ruleId: UUID, notificationService: NotificationService) {
    hookScope.launch {
        runCatching { notifyAbwesenheitChange(ruleId.toString(), notificationService) }
            .onFailure { log.error("notifyAbwesenheitChange failed for rule=$ruleId", it) }
    }
}

private suspend fun notifyEventCreated(eventId: String, notificationService: NotificationService) {
    val recipients = notificationService.resolveRecipients(eventId, "new_event")
    if (recipients.isEmpty()) return

    val event = newSuspendedTransaction {
        EventsTable.selectAll().where { EventsTable.id eq UUID.fromString(eventId) }.singleOrNull()
    } ?: return

    val title = "New event: ${event[EventsTable.title]}"
    val body = "A new event has been added to your schedule."
    val deepLink = "playbook://events/$eventId"

    val timeBucket = event[EventsTable.startAt].toInstant().epochSecond.toString()

    recipients.forEach { uid ->
        val alreadySent = notificationService.dedup(uid, "new_event", eventId, timeBucket)
        if (!alreadySent) {
            notificationService.sendToUser(uid, "new_event", title, body, deepLink, eventId)
        }
    }
}

private suspend fun notifyEventUpdated(eventId: String, notificationService: NotificationService) {
    val recipients = notificationService.resolveRecipients(eventId, "event_changed")
    if (recipients.isEmpty()) return

    val event = newSuspendedTransaction {
        EventsTable.selectAll().where { EventsTable.id eq UUID.fromString(eventId) }.singleOrNull()
    } ?: return

    val title = "Event updated: ${event[EventsTable.title]}"
    val body = "Details for this event have changed."
    val deepLink = "playbook://events/$eventId"

    val timeBucket = event[EventsTable.updatedAt].toInstant().epochSecond.let {
        // Bucket to 5-minute windows to avoid spam on rapid edits
        (it / 300).toString()
    }

    recipients.forEach { uid ->
        val alreadySent = notificationService.dedup(uid, "event_changed", eventId, timeBucket)
        if (!alreadySent) {
            notificationService.sendToUser(uid, "event_changed", title, body, deepLink, eventId)
        }
    }
}

private suspend fun notifyEventCancelled(eventId: String, notificationService: NotificationService) {
    val recipients = notificationService.resolveRecipients(eventId, "event_cancelled")
    if (recipients.isEmpty()) return

    val event = newSuspendedTransaction {
        EventsTable.selectAll().where { EventsTable.id eq UUID.fromString(eventId) }.singleOrNull()
    } ?: return

    val title = "Event cancelled: ${event[EventsTable.title]}"
    val body = "This event has been cancelled."
    val deepLink = "playbook://events/$eventId"
    val timeBucket = "cancelled"

    recipients.forEach { uid ->
        val alreadySent = notificationService.dedup(uid, "event_cancelled", eventId, timeBucket)
        if (!alreadySent) {
            notificationService.sendToUser(uid, "event_cancelled", title, body, deepLink, eventId)
        }
    }
}

private suspend fun notifyAttendanceResponse(
    eventId: String,
    respondingUserId: String,
    status: String,
    notificationService: NotificationService,
) {
    val event = newSuspendedTransaction {
        EventsTable.selectAll().where { EventsTable.id eq UUID.fromString(eventId) }.singleOrNull()
    } ?: return

    // Notify coaches of the event's teams who have attendance_per_response=true
    val eid = UUID.fromString(eventId)
    val coachIds = newSuspendedTransaction {
        val teamIds = EventTeamsTable.selectAll()
            .where { EventTeamsTable.eventId eq eid }
            .map { it[EventTeamsTable.teamId] }

        TeamMembershipsTable.selectAll()
            .where {
                (TeamMembershipsTable.teamId inList teamIds) and
                (TeamMembershipsTable.role eq "coach")
            }
            .map { it[TeamMembershipsTable.userId].toString() }
            .distinct()
            .filter { it != respondingUserId }
    }

    if (coachIds.isEmpty()) return

    val statusLabel = when (status) {
        "confirmed" -> "confirmed attendance"
        "declined" -> "declined"
        "unsure" -> "is unsure"
        else -> "updated attendance"
    }
    val title = "Attendance update"
    val body = "A player $statusLabel for ${event[EventsTable.title]}."
    val deepLink = "playbook://events/$eventId/attendance"
    val timeBucket = (System.currentTimeMillis() / 300_000).toString()

    val eligibleCoaches = coachIds.filter { uid ->
        !notificationService.dedup(uid, "attendance_response", "$eventId:$respondingUserId", timeBucket)
    }

    if (eligibleCoaches.isNotEmpty()) {
        notificationService.sendToUsers(
            eligibleCoaches,
            "attendance_response",
            title,
            body,
            deepLink,
            eventId,
        )
    }
}

private suspend fun notifyAbwesenheitChange(ruleId: String, notificationService: NotificationService) {
    val rule = newSuspendedTransaction {
        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq UUID.fromString(ruleId) }
            .singleOrNull()
    } ?: return

    val userId = rule[AbwesenheitRulesTable.userId].toString()
    val title = "Absence rule updated"
    val body = "Your absence rule has been updated. Future events will be adjusted."
    val deepLink = "playbook://absences"
    val timeBucket = (System.currentTimeMillis() / 300_000).toString()

    val alreadySent = notificationService.dedup(userId, "abwesenheit_changed", ruleId, timeBucket)
    if (!alreadySent) {
        notificationService.sendToUser(userId, "abwesenheit_changed", title, body, deepLink, ruleId)
    }
}
