package com.playbook.infra

import com.playbook.db.tables.AbwesenheitBackfillJobsTable
import com.playbook.db.tables.AbwesenheitRulesTable
import com.playbook.db.tables.AttendanceResponsesTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.TeamMembershipsTable
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * T-017: Processes a single backfill job for an abwesenheit rule.
 * Upserts declined-auto attendance_responses for all matching future events.
 * Skips rows where manual_override=true.
 */
fun Application.launchAbwesenheitBackfill(jobId: UUID, ruleId: UUID) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching { processBackfillJob(jobId, ruleId) }
            .onFailure {
                log.error("Backfill job $jobId failed", it)
                newSuspendedTransaction {
                    val now = OffsetDateTime.now(ZoneOffset.UTC)
                    AbwesenheitBackfillJobsTable.update({ AbwesenheitBackfillJobsTable.id eq jobId }) {
                        it[status] = "failed"
                        it[updatedAt] = now
                    }
                }
            }
    }
}

private suspend fun processBackfillJob(jobId: UUID, ruleId: UUID) {
    newSuspendedTransaction {
        AbwesenheitBackfillJobsTable.update({ AbwesenheitBackfillJobsTable.id eq jobId }) {
            it[status] = "processing"
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val today = now.date

    val rule = newSuspendedTransaction {
        AbwesenheitRulesTable.selectAll().where { AbwesenheitRulesTable.id eq ruleId }.singleOrNull()
    } ?: return

    val userId = rule[AbwesenheitRulesTable.userId]
    val ruleType = rule[AbwesenheitRulesTable.ruleType]
    val weekdaysText = rule[AbwesenheitRulesTable.weekdays]
    val weekdays = weekdaysText?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    val startDate = rule[AbwesenheitRulesTable.startDate]
    val endDate = rule[AbwesenheitRulesTable.endDate]

    val futureEvents = newSuspendedTransaction {
        TeamMembershipsTable
            .join(EventTeamsTable, org.jetbrains.exposed.sql.JoinType.INNER, TeamMembershipsTable.teamId, EventTeamsTable.teamId)
            .join(EventsTable, org.jetbrains.exposed.sql.JoinType.INNER, EventTeamsTable.eventId, EventsTable.id)
            .selectAll().where {
                (TeamMembershipsTable.userId eq userId) and
                (EventsTable.startAt greaterEq OffsetDateTime.now(ZoneOffset.UTC)) and
                (EventsTable.status eq "active")
            }
            .map { row ->
                val eventId = row[EventsTable.id]
                val startAt = row[EventsTable.startAt].toInstant()
                val date = kotlinx.datetime.Instant
                    .fromEpochMilliseconds(startAt.toEpochMilli())
                    .toLocalDateTime(TimeZone.UTC).date
                Pair(eventId, date)
            }
    }

    val matchingEventIds = futureEvents.filter { (_, date) ->
        when (ruleType) {
            "recurring" -> {
                val dayOfWeek = date.dayOfWeek.ordinal // Mon=0
                dayOfWeek in weekdays && (endDate == null || date <= endDate)
            }
            "period" -> {
                startDate != null && date >= startDate && (endDate == null || date <= endDate)
            }
            else -> false
        }
    }.map { it.first }

    val nowTs = OffsetDateTime.now(ZoneOffset.UTC)
    newSuspendedTransaction {
        matchingEventIds.forEach { eventId ->
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
                    it[updatedAt] = nowTs
                }
            } else if (!existing[AttendanceResponsesTable.manualOverride]) {
                AttendanceResponsesTable.update({
                    (AttendanceResponsesTable.eventId eq eventId) and
                    (AttendanceResponsesTable.userId eq userId)
                }) {
                    it[status] = "declined-auto"
                    it[abwesenheitRuleId] = ruleId
                    it[updatedAt] = nowTs
                }
            }
        }

        AbwesenheitBackfillJobsTable.update({ AbwesenheitBackfillJobsTable.id eq jobId }) {
            it[status] = "done"
            it[updatedAt] = nowTs
        }
    }
}
