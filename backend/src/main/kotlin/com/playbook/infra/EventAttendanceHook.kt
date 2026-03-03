package com.playbook.infra

import com.playbook.db.tables.AbwesenheitRulesTable
import com.playbook.db.tables.AttendanceResponsesTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.TeamMembershipsTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * T-019: Called after event creation to auto-decline for matching abwesenheit rules.
 */
fun launchBackfillForNewEvent(eventId: UUID, teamIds: List<UUID>) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching { backfillForNewEvent(eventId, teamIds) }
        // Errors are non-critical; silently ignored here
    }
}

private suspend fun backfillForNewEvent(eventId: UUID, teamIds: List<UUID>) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val eventRow = newSuspendedTransaction {
        EventsTable.selectAll().where { EventsTable.id eq eventId }.singleOrNull()
    } ?: return

    val startAt = eventRow[EventsTable.startAt]
    val eventDate = kotlinx.datetime.Instant
        .fromEpochMilliseconds(startAt.toInstant().toEpochMilli())
        .toLocalDateTime(TimeZone.UTC).date
    val dayOfWeek = eventDate.dayOfWeek.ordinal // Mon=0

    val userIds = newSuspendedTransaction {
        TeamMembershipsTable.selectAll().where { TeamMembershipsTable.teamId inList teamIds }
            .map { it[TeamMembershipsTable.userId] }
    }

    val rules = newSuspendedTransaction {
        AbwesenheitRulesTable.selectAll().where { AbwesenheitRulesTable.userId inList userIds }.toList()
    }

    newSuspendedTransaction {
        rules.forEach { rule ->
            val userId = rule[AbwesenheitRulesTable.userId]
            val ruleId = rule[AbwesenheitRulesTable.id]
            val ruleType = rule[AbwesenheitRulesTable.ruleType]
            val weekdaysText = rule[AbwesenheitRulesTable.weekdays]
            val weekdays = weekdaysText?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
            val startDate = rule[AbwesenheitRulesTable.startDate]
            val endDate = rule[AbwesenheitRulesTable.endDate]

            val matches = when (ruleType) {
                "recurring" -> dayOfWeek in weekdays && (endDate == null || eventDate <= endDate)
                "period" -> startDate != null && eventDate >= startDate && (endDate == null || eventDate <= endDate)
                else -> false
            }

            if (matches) {
                val existing = AttendanceResponsesTable.selectAll().where {
                    (AttendanceResponsesTable.eventId eq eventId) and
                    (AttendanceResponsesTable.userId eq userId)
                }.singleOrNull()

                if (existing == null) {
                    AttendanceResponsesTable.insert {
                        it[AttendanceResponsesTable.eventId] = eventId
                        it[AttendanceResponsesTable.userId] = userId
                        it[status] = "declined-auto"
                        it[abwesenheitRuleId] = ruleId
                        it[manualOverride] = false
                        it[updatedAt] = now
                    }
                }
            }
        }
    }
}
