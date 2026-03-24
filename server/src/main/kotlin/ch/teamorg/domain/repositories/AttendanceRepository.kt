package ch.teamorg.domain.repositories

import java.time.Instant
import java.util.UUID

data class AttendanceResponseRow(
    val eventId: UUID,
    val userId: UUID,
    val status: String,
    val reason: String?,
    val abwesenheitRuleId: UUID?,
    val manualOverride: Boolean,
    val respondedAt: Instant?,
    val updatedAt: Instant
)

data class CheckInRow(
    val eventId: UUID,
    val userId: UUID,
    val status: String,
    val note: String?,
    val setBy: UUID,
    val setAt: Instant,
    val previousStatus: String?,
    val previousSetBy: UUID?
)

data class RawAttendanceRow(
    val eventId: UUID,
    val userId: UUID,
    val responseStatus: String?,
    val recordStatus: String?,
    val eventStartAt: Instant
)

interface AttendanceRepository {
    suspend fun getEventAttendance(eventId: UUID): List<AttendanceResponseRow>
    suspend fun getMyResponse(eventId: UUID, userId: UUID): AttendanceResponseRow?
    suspend fun upsertResponse(eventId: UUID, userId: UUID, status: String, reason: String?): AttendanceResponseRow
    suspend fun isDeadlinePassed(eventId: UUID): Boolean
    suspend fun getCheckIn(eventId: UUID): List<CheckInRow>
    suspend fun upsertCheckIn(eventId: UUID, userId: UUID, status: String, note: String?, setBy: UUID): CheckInRow
    suspend fun getRawAttendance(userId: UUID, from: Instant?, to: Instant?): List<RawAttendanceRow>
    suspend fun getTeamAttendance(teamId: UUID, from: Instant?, to: Instant?): List<RawAttendanceRow>
    suspend fun bulkInsertAutoDeclines(ruleId: UUID, userId: UUID, eventUserPairs: List<Pair<UUID, UUID>>)
}
