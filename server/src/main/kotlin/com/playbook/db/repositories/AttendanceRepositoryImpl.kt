package com.playbook.db.repositories

import com.playbook.db.tables.AttendanceRecordsTable
import com.playbook.db.tables.AttendanceResponsesTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.db.tables.UsersTable
import com.playbook.domain.AttendanceEntry
import com.playbook.domain.AttendanceRecord
import com.playbook.domain.AttendanceRecordStatus
import com.playbook.domain.AttendanceResponse
import com.playbook.domain.AttendanceResponseStatus
import com.playbook.domain.AttendanceRow
import com.playbook.domain.EventType
import com.playbook.domain.TeamAttendanceView
import com.playbook.domain.UpdateAttendanceRequest
import com.playbook.domain.UpdateCheckInRequest
import com.playbook.plugins.ConflictException
import com.playbook.repository.AttendanceRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AttendanceRepositoryImpl : AttendanceRepository {

    override suspend fun getEventAttendance(eventId: String): TeamAttendanceView = newSuspendedTransaction {
        val eid = UUID.fromString(eventId)

        // Get all team members for this event's teams
        val teamIds = EventTeamsTable.selectAll().where { EventTeamsTable.eventId eq eid }
            .map { it[EventTeamsTable.teamId] }

        val memberUserIds = TeamMembershipsTable.selectAll()
            .where { TeamMembershipsTable.teamId inList teamIds }
            .map { it[TeamMembershipsTable.userId] }
            .distinct()

        val displayNameByUser = UsersTable.selectAll()
            .where { UsersTable.id inList memberUserIds }
            .associate { it[UsersTable.id] to (it[UsersTable.displayName] ?: "") }

        // Load all responses for this event
        val responsesByUser = AttendanceResponsesTable.selectAll()
            .where { AttendanceResponsesTable.eventId eq eid }
            .associate { it[AttendanceResponsesTable.userId] to it.toAttendanceResponse() }

        // Load all records for this event
        val recordsByUser = AttendanceRecordsTable.selectAll()
            .where { AttendanceRecordsTable.eventId eq eid }
            .associate { it[AttendanceRecordsTable.userId] to it.toAttendanceRecord() }

        val entries = memberUserIds.map { userId ->
            AttendanceEntry(
                userId = userId.toString(),
                displayName = displayNameByUser[userId] ?: "",
                response = responsesByUser[userId],
                record = recordsByUser[userId],
            )
        }

        val confirmed = entries.filter { it.response?.status == AttendanceResponseStatus.CONFIRMED }
        val declined = entries.filter {
            it.response?.status == AttendanceResponseStatus.DECLINED ||
            it.response?.status == AttendanceResponseStatus.DECLINED_AUTO
        }
        val unsure = entries.filter { it.response?.status == AttendanceResponseStatus.UNSURE }
        val noResponse = entries.filter {
            it.response == null || it.response?.status == AttendanceResponseStatus.NO_RESPONSE
        }

        TeamAttendanceView(
            confirmed = confirmed,
            declined = declined,
            unsure = unsure,
            noResponse = noResponse,
        )
    }

    override suspend fun getMyAttendance(eventId: String, userId: String): AttendanceResponse = newSuspendedTransaction {
        val eid = UUID.fromString(eventId)
        val uid = UUID.fromString(userId)
        AttendanceResponsesTable.selectAll()
            .where { (AttendanceResponsesTable.eventId eq eid) and (AttendanceResponsesTable.userId eq uid) }
            .singleOrNull()
            ?.toAttendanceResponse()
            ?: AttendanceResponse(
                eventId = eventId,
                userId = userId,
                status = AttendanceResponseStatus.NO_RESPONSE,
                reason = null,
                abwesenheitRuleId = null,
                manualOverride = false,
                respondedAt = null,
                updatedAt = Clock.System.now(),
            )
    }

    override suspend fun upsertAttendance(
        eventId: String,
        userId: String,
        request: UpdateAttendanceRequest,
    ): AttendanceResponse = newSuspendedTransaction {
        if (request.status == AttendanceResponseStatus.UNSURE && request.reason == null) {
            throw IllegalArgumentException("reason required for unsure")
        }

        val eid = UUID.fromString(eventId)
        val uid = UUID.fromString(userId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Check deadline: if now > event endAt, deadline passed
        val event = EventsTable.selectAll().where { EventsTable.id eq eid }.singleOrNull()
            ?: throw IllegalArgumentException("Event not found")
        val eventEnd = event[EventsTable.endAt]
        if (now.isAfter(eventEnd)) {
            throw ConflictException("Deadline has passed")
        }

        val statusStr = request.status.toDbString()

        val existing = AttendanceResponsesTable.selectAll()
            .where { (AttendanceResponsesTable.eventId eq eid) and (AttendanceResponsesTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            AttendanceResponsesTable.insert {
                it[AttendanceResponsesTable.eventId] = eid
                it[AttendanceResponsesTable.userId] = uid
                it[status] = statusStr
                it[reason] = request.reason
                it[abwesenheitRuleId] = null
                it[manualOverride] = false
                it[respondedAt] = now
                it[updatedAt] = now
            }
        } else {
            AttendanceResponsesTable.update({
                (AttendanceResponsesTable.eventId eq eid) and (AttendanceResponsesTable.userId eq uid)
            }) {
                it[status] = statusStr
                it[reason] = request.reason
                it[manualOverride] = true
                it[respondedAt] = existing[AttendanceResponsesTable.respondedAt] ?: now
                it[updatedAt] = now
            }
        }

        AttendanceResponsesTable.selectAll()
            .where { (AttendanceResponsesTable.eventId eq eid) and (AttendanceResponsesTable.userId eq uid) }
            .single()
            .toAttendanceResponse()
    }

    override suspend fun getCheckInList(eventId: String): List<AttendanceEntry> = newSuspendedTransaction {
        val eid = UUID.fromString(eventId)

        val teamIds = EventTeamsTable.selectAll().where { EventTeamsTable.eventId eq eid }
            .map { it[EventTeamsTable.teamId] }

        val memberUserIds = TeamMembershipsTable.selectAll()
            .where { TeamMembershipsTable.teamId inList teamIds }
            .map { it[TeamMembershipsTable.userId] }
            .distinct()

        val displayNameByUser = UsersTable.selectAll()
            .where { UsersTable.id inList memberUserIds }
            .associate { it[UsersTable.id] to (it[UsersTable.displayName] ?: "") }

        val responsesByUser = AttendanceResponsesTable.selectAll()
            .where { AttendanceResponsesTable.eventId eq eid }
            .associate { it[AttendanceResponsesTable.userId] to it.toAttendanceResponse() }

        val recordsByUser = AttendanceRecordsTable.selectAll()
            .where { AttendanceRecordsTable.eventId eq eid }
            .associate { it[AttendanceRecordsTable.userId] to it.toAttendanceRecord() }

        memberUserIds.map { userId ->
            AttendanceEntry(
                userId = userId.toString(),
                displayName = displayNameByUser[userId] ?: "",
                response = responsesByUser[userId],
                record = recordsByUser[userId],
            )
        }
    }

    override suspend fun setCheckIn(
        eventId: String,
        userId: String,
        request: UpdateCheckInRequest,
    ): AttendanceRecord = newSuspendedTransaction {
        val eid = UUID.fromString(eventId)
        val uid = UUID.fromString(userId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val statusStr = request.status.toDbString()

        val existing = AttendanceRecordsTable.selectAll()
            .where { (AttendanceRecordsTable.eventId eq eid) and (AttendanceRecordsTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            AttendanceRecordsTable.insert {
                it[AttendanceRecordsTable.eventId] = eid
                it[AttendanceRecordsTable.userId] = uid
                it[status] = statusStr
                it[note] = request.note
                it[setBy] = uid
                it[setAt] = now
                it[previousStatus] = null
                it[previousSetBy] = null
            }
        } else {
            AttendanceRecordsTable.update({
                (AttendanceRecordsTable.eventId eq eid) and (AttendanceRecordsTable.userId eq uid)
            }) {
                it[previousStatus] = existing[AttendanceRecordsTable.status]
                it[previousSetBy] = existing[AttendanceRecordsTable.setBy]
                it[status] = statusStr
                it[note] = request.note
                it[setBy] = uid
                it[setAt] = now
            }
        }

        AttendanceRecordsTable.selectAll()
            .where { (AttendanceRecordsTable.eventId eq eid) and (AttendanceRecordsTable.userId eq uid) }
            .single()
            .toAttendanceRecord()
    }

    override suspend fun getUserAttendance(
        userId: String,
        from: Instant?,
        to: Instant?,
    ): List<AttendanceRow> = newSuspendedTransaction {
        val uid = UUID.fromString(userId)

        val query = EventsTable
            .join(EventTeamsTable, JoinType.INNER, EventsTable.id, EventTeamsTable.eventId)
            .join(TeamMembershipsTable, JoinType.INNER, EventTeamsTable.teamId, TeamMembershipsTable.teamId)
            .selectAll()
            .where {
                var cond = TeamMembershipsTable.userId eq uid
                from?.let { f -> cond = cond and (EventsTable.startAt greaterEq f.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                to?.let { t -> cond = cond and (EventsTable.startAt lessEq t.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                cond
            }

        val eventRows = query.map { it[EventsTable.id] }.distinct()

        val responsesByEvent = AttendanceResponsesTable.selectAll()
            .where { (AttendanceResponsesTable.userId eq uid) and (AttendanceResponsesTable.eventId inList eventRows) }
            .associate { it[AttendanceResponsesTable.eventId] to it.toAttendanceResponse() }

        val recordsByEvent = AttendanceRecordsTable.selectAll()
            .where { (AttendanceRecordsTable.userId eq uid) and (AttendanceRecordsTable.eventId inList eventRows) }
            .associate { it[AttendanceRecordsTable.eventId] to it.toAttendanceRecord() }

        query.distinctBy { it[EventsTable.id] }.map { row ->
            val eid = row[EventsTable.id]
            AttendanceRow(
                eventId = eid.toString(),
                eventType = row[EventsTable.type].toEventType(),
                eventDate = row[EventsTable.startAt].toInstant().toKotlinInstant(),
                response = responsesByEvent[eid],
                record = recordsByEvent[eid],
            )
        }
    }

    override suspend fun getTeamAttendance(
        teamId: String,
        from: Instant?,
        to: Instant?,
    ): List<AttendanceRow> = newSuspendedTransaction {
        val tid = UUID.fromString(teamId)

        val memberIds = TeamMembershipsTable.selectAll()
            .where { TeamMembershipsTable.teamId eq tid }
            .map { it[TeamMembershipsTable.userId] }

        val query = EventsTable
            .join(EventTeamsTable, JoinType.INNER, EventsTable.id, EventTeamsTable.eventId)
            .selectAll()
            .where {
                var cond = EventTeamsTable.teamId eq tid
                from?.let { f -> cond = cond and (EventsTable.startAt greaterEq f.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                to?.let { t -> cond = cond and (EventsTable.startAt lessEq t.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                cond
            }

        val eventIds = query.map { it[EventsTable.id] }.distinct()

        val responsesByEventAndUser = AttendanceResponsesTable.selectAll()
            .where {
                (AttendanceResponsesTable.eventId inList eventIds) and
                (AttendanceResponsesTable.userId inList memberIds)
            }
            .groupBy { it[AttendanceResponsesTable.eventId] }
            .mapValues { (_, rows) -> rows.associate { it[AttendanceResponsesTable.userId] to it.toAttendanceResponse() } }

        val recordsByEventAndUser = AttendanceRecordsTable.selectAll()
            .where {
                (AttendanceRecordsTable.eventId inList eventIds) and
                (AttendanceRecordsTable.userId inList memberIds)
            }
            .groupBy { it[AttendanceRecordsTable.eventId] }
            .mapValues { (_, rows) -> rows.associate { it[AttendanceRecordsTable.userId] to it.toAttendanceRecord() } }

        // Return one row per (event, user) combination
        query.distinctBy { it[EventsTable.id] }.flatMap { row ->
            val eid = row[EventsTable.id]
            memberIds.map { uid ->
                AttendanceRow(
                    eventId = eid.toString(),
                    userId = uid.toString(),
                    eventType = row[EventsTable.type].toEventType(),
                    eventDate = row[EventsTable.startAt].toInstant().toKotlinInstant(),
                    response = responsesByEventAndUser[eid]?.get(uid),
                    record = recordsByEventAndUser[eid]?.get(uid),
                )
            }
        }
    }

    override fun observeMyAttendance(eventId: String): kotlinx.coroutines.flow.Flow<AttendanceResponse> =
        throw UnsupportedOperationException("observeMyAttendance is not supported on the server")

    override suspend fun triggerBackfillForEvent(eventId: String, teamId: String) {
        // Delegates to EventAttendanceHook; no-op here as hook is called from EventRepositoryImpl
    }

    // --- Mappers ---

    private fun ResultRow.toAttendanceResponse() = AttendanceResponse(
        eventId = this[AttendanceResponsesTable.eventId].toString(),
        userId = this[AttendanceResponsesTable.userId].toString(),
        status = this[AttendanceResponsesTable.status].toResponseStatus(),
        reason = this[AttendanceResponsesTable.reason],
        abwesenheitRuleId = this[AttendanceResponsesTable.abwesenheitRuleId]?.toString(),
        manualOverride = this[AttendanceResponsesTable.manualOverride],
        respondedAt = this[AttendanceResponsesTable.respondedAt]?.toInstant()?.toKotlinInstant(),
        updatedAt = this[AttendanceResponsesTable.updatedAt].toInstant().toKotlinInstant(),
    )

    private fun ResultRow.toAttendanceRecord() = AttendanceRecord(
        eventId = this[AttendanceRecordsTable.eventId].toString(),
        userId = this[AttendanceRecordsTable.userId].toString(),
        status = this[AttendanceRecordsTable.status].toRecordStatus(),
        note = this[AttendanceRecordsTable.note],
        setBy = this[AttendanceRecordsTable.setBy].toString(),
        setAt = this[AttendanceRecordsTable.setAt].toInstant().toKotlinInstant(),
        previousStatus = this[AttendanceRecordsTable.previousStatus]?.toRecordStatus(),
        previousSetBy = this[AttendanceRecordsTable.previousSetBy]?.toString(),
    )

    private fun String.toResponseStatus() = when (this) {
        "confirmed" -> AttendanceResponseStatus.CONFIRMED
        "declined" -> AttendanceResponseStatus.DECLINED
        "unsure" -> AttendanceResponseStatus.UNSURE
        "declined-auto" -> AttendanceResponseStatus.DECLINED_AUTO
        else -> AttendanceResponseStatus.NO_RESPONSE
    }

    private fun String.toRecordStatus() = when (this) {
        "present" -> AttendanceRecordStatus.PRESENT
        "absent" -> AttendanceRecordStatus.ABSENT
        else -> AttendanceRecordStatus.EXCUSED
    }

    private fun AttendanceResponseStatus.toDbString() = when (this) {
        AttendanceResponseStatus.CONFIRMED -> "confirmed"
        AttendanceResponseStatus.DECLINED -> "declined"
        AttendanceResponseStatus.UNSURE -> "unsure"
        AttendanceResponseStatus.DECLINED_AUTO -> "declined-auto"
        AttendanceResponseStatus.NO_RESPONSE -> "no-response"
    }

    private fun AttendanceRecordStatus.toDbString() = when (this) {
        AttendanceRecordStatus.PRESENT -> "present"
        AttendanceRecordStatus.ABSENT -> "absent"
        AttendanceRecordStatus.EXCUSED -> "excused"
    }

    private fun String.toEventType() = when (this) {
        "match" -> EventType.MATCH
        "other" -> EventType.OTHER
        else -> EventType.TRAINING
    }

    private fun Instant.toJavaInstant(): java.time.Instant = java.time.Instant.ofEpochMilli(toEpochMilliseconds())
}
