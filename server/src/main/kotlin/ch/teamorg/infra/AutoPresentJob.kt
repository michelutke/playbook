package ch.teamorg.infra

import ch.teamorg.db.tables.AttendanceRecordsTable
import ch.teamorg.db.tables.AttendanceResponsesTable
import ch.teamorg.db.tables.EventStatus
import ch.teamorg.db.tables.EventsTable
import ch.teamorg.db.tables.RecordStatus
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

private val logger = LoggerFactory.getLogger("AutoPresentJob")

// Sentinel UUID representing SYSTEM automated actions
private val SYSTEM_UUID = UUID(0L, 0L)

fun Application.startAutoPresentJob() {
    launch(Dispatchers.IO) {
        while (isActive) {
            delay(15.minutes)
            try {
                runAutoPresentCheck()
            } catch (e: Exception) {
                logger.error("Auto-present job failed", e)
            }
        }
    }
}

private fun runAutoPresentCheck() {
    transaction {
        val now = Instant.now()

        // Find events that ended, are active, and don't use manual check-in
        val eligibleEvents = EventsTable.selectAll()
            .where {
                (EventsTable.endAt lessEq now) and
                (EventsTable.status eq EventStatus.active) and
                (EventsTable.checkInEnabled eq false)
            }
            .map { it[EventsTable.id] }

        var processed = 0
        for (eventId in eligibleEvents) {
            // Find confirmed responses for this event
            val confirmedUsers = AttendanceResponsesTable.selectAll()
                .where {
                    (AttendanceResponsesTable.eventId eq eventId) and
                    (AttendanceResponsesTable.status eq "confirmed")
                }
                .map { it[AttendanceResponsesTable.userId] }

            val setAt = Instant.now()
            for (userId in confirmedUsers) {
                // ON CONFLICT DO NOTHING — only insert if no coach record exists
                val alreadyRecorded = AttendanceRecordsTable.selectAll()
                    .where {
                        (AttendanceRecordsTable.eventId eq eventId) and
                        (AttendanceRecordsTable.userId eq userId)
                    }
                    .count() > 0
                if (alreadyRecorded) continue

                AttendanceRecordsTable.insert {
                    it[AttendanceRecordsTable.eventId] = eventId
                    it[AttendanceRecordsTable.userId] = userId
                    it[AttendanceRecordsTable.status] = RecordStatus.present
                    it[AttendanceRecordsTable.setBy] = SYSTEM_UUID
                    it[AttendanceRecordsTable.setAt] = setAt
                }
                processed++
            }
        }

        if (processed > 0) {
            logger.info("Auto-present: inserted $processed present records for ${eligibleEvents.size} events")
        }
    }
}
