package com.playbook.infra

import com.playbook.db.tables.AttendanceRecordsTable
import com.playbook.db.tables.AttendanceResponsesTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.TeamsTable
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * T-018: Every 5 min, auto-sets present for events that just ended
 * where check_in_enabled=false and response=confirmed.
 */
fun Application.startAutoPresentJob() {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        while (isActive) {
            runCatching { processAutoPresent() }
                .onFailure { log.error("AutoPresent job failed", it) }
            delay(TimeUnit.MINUTES.toMillis(5))
        }
    }
}

private suspend fun processAutoPresent() {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val windowStart = now.minusMinutes(5)

    val eventsEnded = newSuspendedTransaction {
        EventTeamsTable
            .join(EventsTable, JoinType.INNER, EventTeamsTable.eventId, EventsTable.id)
            .join(TeamsTable, JoinType.INNER, EventTeamsTable.teamId, TeamsTable.id)
            .selectAll().where {
                (EventsTable.endAt greaterEq windowStart) and
                (EventsTable.endAt lessEq now) and
                (EventsTable.status eq "active") and
                (TeamsTable.checkInEnabled eq false)
            }
            .map { it[EventsTable.id] }
            .distinct()
    }

    if (eventsEnded.isEmpty()) return

    newSuspendedTransaction {
        eventsEnded.forEach { eventId ->
            val confirmedUsers = AttendanceResponsesTable.selectAll().where {
                (AttendanceResponsesTable.eventId eq eventId) and
                (AttendanceResponsesTable.status eq "confirmed")
            }.map { it[AttendanceResponsesTable.userId] }

            confirmedUsers.forEach { userId ->
                val existing = AttendanceRecordsTable.selectAll().where {
                    (AttendanceRecordsTable.eventId eq eventId) and
                    (AttendanceRecordsTable.userId eq userId)
                }.singleOrNull()

                if (existing == null) {
                    AttendanceRecordsTable.insert {
                        it[AttendanceRecordsTable.eventId] = eventId
                        it[AttendanceRecordsTable.userId] = userId
                        it[status] = "present"
                        it[setBy] = userId
                        it[setAt] = now
                    }
                }
            }
        }
    }
}
