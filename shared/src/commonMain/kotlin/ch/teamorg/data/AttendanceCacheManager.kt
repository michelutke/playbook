package ch.teamorg.data

import ch.teamorg.db.TeamorgDb
import ch.teamorg.domain.AbwesenheitRule
import ch.teamorg.domain.AttendanceResponse
import kotlinx.datetime.Instant

class AttendanceCacheManager(private val db: TeamorgDb) {

    fun saveResponses(eventId: String, responses: List<AttendanceResponse>) {
        db.attendanceQueries.transaction {
            db.attendanceQueries.deleteEventResponses(eventId)
            responses.forEach { r ->
                db.attendanceQueries.upsertResponse(
                    event_id = r.eventId,
                    user_id = r.userId,
                    status = r.status,
                    reason = r.reason,
                    abwesenheit_rule_id = r.abwesenheitRuleId,
                    manual_override = if (r.manualOverride) 1L else 0L,
                    responded_at = r.respondedAt?.toEpochMilliseconds(),
                    updated_at = r.updatedAt.toEpochMilliseconds()
                )
            }
        }
    }

    fun getCachedResponses(eventId: String): List<AttendanceResponse> {
        return db.attendanceQueries.getEventResponses(eventId).executeAsList().map { row ->
            AttendanceResponse(
                eventId = row.event_id,
                userId = row.user_id,
                status = row.status,
                reason = row.reason,
                abwesenheitRuleId = row.abwesenheit_rule_id,
                manualOverride = row.manual_override != 0L,
                respondedAt = row.responded_at?.let { Instant.fromEpochMilliseconds(it) },
                updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
            )
        }
    }

    fun getCachedResponse(eventId: String, userId: String): AttendanceResponse? {
        return db.attendanceQueries.getMyResponse(eventId, userId).executeAsOneOrNull()?.let { row ->
            AttendanceResponse(
                eventId = row.event_id,
                userId = row.user_id,
                status = row.status,
                reason = row.reason,
                abwesenheitRuleId = row.abwesenheit_rule_id,
                manualOverride = row.manual_override != 0L,
                respondedAt = row.responded_at?.let { Instant.fromEpochMilliseconds(it) },
                updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
            )
        }
    }

    fun saveRules(userId: String, rules: List<AbwesenheitRule>) {
        db.attendanceQueries.transaction {
            db.attendanceQueries.clearAllRules(userId)
            rules.forEach { r ->
                db.attendanceQueries.upsertRule(
                    id = r.id,
                    user_id = r.userId,
                    preset_type = r.presetType,
                    label = r.label,
                    body_part = r.bodyPart,
                    rule_type = r.ruleType,
                    weekdays = r.weekdays?.joinToString(","),
                    start_date = r.startDate,
                    end_date = r.endDate,
                    created_at = r.createdAt,
                    updated_at = r.updatedAt
                )
            }
        }
    }

    fun getCachedRules(userId: String): List<AbwesenheitRule> {
        return db.attendanceQueries.getAllRules(userId).executeAsList().map { row ->
            AbwesenheitRule(
                id = row.id,
                userId = row.user_id,
                presetType = row.preset_type,
                label = row.label,
                bodyPart = row.body_part,
                ruleType = row.rule_type,
                weekdays = row.weekdays?.split(",")?.map { it.trim().toInt() },
                startDate = row.start_date,
                endDate = row.end_date,
                createdAt = row.created_at,
                updatedAt = row.updated_at
            )
        }
    }

    fun deleteRule(ruleId: String) {
        db.attendanceQueries.deleteRule(ruleId)
    }
}
