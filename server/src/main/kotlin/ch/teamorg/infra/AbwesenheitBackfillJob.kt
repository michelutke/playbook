package ch.teamorg.infra

import ch.teamorg.db.tables.AbwesenheitRulesTable
import ch.teamorg.db.tables.AttendanceResponsesTable
import ch.teamorg.db.tables.EventTeamsTable
import ch.teamorg.db.tables.EventsTable
import ch.teamorg.db.tables.RuleType
import ch.teamorg.db.tables.TeamRolesTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("AbwesenheitBackfillJob")

class AbwesenheitBackfillJob {
    enum class Status { PENDING, DONE, FAILED }

    private val statusMap = ConcurrentHashMap<UUID, Status>()

    fun getStatus(userId: UUID): Status = statusMap[userId] ?: Status.DONE

    fun enqueue(userId: UUID, ruleId: UUID, scope: Application) {
        statusMap[userId] = Status.PENDING
        scope.launch(Dispatchers.IO) {
            try {
                runBackfill(userId, ruleId)
                statusMap[userId] = Status.DONE
            } catch (e: Exception) {
                logger.error("Backfill failed for user=$userId rule=$ruleId", e)
                statusMap[userId] = Status.FAILED
            }
        }
    }

    private fun runBackfill(userId: UUID, ruleId: UUID) {
        transaction {
            val rule = AbwesenheitRulesTable.selectAll()
                .where { AbwesenheitRulesTable.id eq ruleId }
                .singleOrNull() ?: return@transaction

            val ruleType = rule[AbwesenheitRulesTable.ruleType]
            val weekdays = rule[AbwesenheitRulesTable.weekdays]
            val startDate = rule[AbwesenheitRulesTable.startDate]
            val endDate = rule[AbwesenheitRulesTable.endDate]
            val now = Instant.now()

            // Find all teams the user belongs to
            val teamIds = TeamRolesTable.selectAll()
                .where { TeamRolesTable.userId eq userId }
                .map { it[TeamRolesTable.teamId] }

            if (teamIds.isEmpty()) return@transaction

            // Find all future active events for those teams
            val futureEvents = (EventsTable innerJoin EventTeamsTable).selectAll()
                .where {
                    (EventTeamsTable.teamId inList teamIds) and
                    (EventsTable.startAt greaterEq now) and
                    (EventsTable.status eq ch.teamorg.db.tables.EventStatus.active)
                }
                .map { it[EventsTable.id] to it[EventsTable.startAt] }
                .distinctBy { it.first }

            // Filter events matching rule
            val matchingEventIds = futureEvents.filter { (_, startAt) ->
                val eventDate = startAt.atZone(ZoneOffset.UTC).toLocalDate()
                when (ruleType) {
                    RuleType.recurring -> {
                        val dayOfWeek = eventDate.dayOfWeek.value % 7 // Mon=1..Sun=7 -> 0=Mon..6=Sun
                        val matchesDay = weekdays != null && weekdays.any { it.toInt() == dayOfWeek }
                        val beforeEnd = endDate == null || !eventDate.isAfter(endDate)
                        matchesDay && beforeEnd
                    }
                    RuleType.period -> {
                        val afterStart = startDate == null || !eventDate.isBefore(startDate)
                        val beforeEnd = endDate == null || !eventDate.isAfter(endDate)
                        afterStart && beforeEnd
                    }
                }
            }.map { (eventId, _) -> eventId }

            // Bulk insert declined-auto for matching events, skip manual overrides
            for (eventId in matchingEventIds) {
                val hasManualOverride = AttendanceResponsesTable.selectAll()
                    .where {
                        (AttendanceResponsesTable.eventId eq eventId) and
                        (AttendanceResponsesTable.userId eq userId) and
                        (AttendanceResponsesTable.manualOverride eq true)
                    }
                    .count() > 0
                if (hasManualOverride) continue

                AttendanceResponsesTable.upsert(
                    keys = arrayOf(AttendanceResponsesTable.eventId, AttendanceResponsesTable.userId)
                ) {
                    it[AttendanceResponsesTable.eventId] = eventId
                    it[AttendanceResponsesTable.userId] = userId
                    it[AttendanceResponsesTable.status] = "declined-auto"
                    it[AttendanceResponsesTable.abwesenheitRuleId] = ruleId
                    it[AttendanceResponsesTable.manualOverride] = false
                    it[AttendanceResponsesTable.updatedAt] = Instant.now()
                }
            }

            logger.info("Backfill complete for user=$userId rule=$ruleId — processed ${matchingEventIds.size} events")
        }
    }
}
