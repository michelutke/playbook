package com.playbook.db.repositories

import com.playbook.db.tables.EventSeriesTable
import com.playbook.db.tables.EventSubgroupsTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import com.playbook.db.tables.SubgroupMembersTable
import com.playbook.db.tables.SubgroupsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.db.tables.TeamsTable
import com.playbook.domain.CancelEventRequest
import com.playbook.domain.CreateEventRequest
import com.playbook.domain.Event
import com.playbook.domain.EventStatus
import com.playbook.domain.EventType
import com.playbook.domain.RecurringPattern
import com.playbook.domain.RecurringScope
import com.playbook.domain.SubgroupRef
import com.playbook.domain.TeamRef
import com.playbook.domain.UpdateEventRequest
import com.playbook.repository.EventRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class EventRepositoryImpl : EventRepository {

    // ES-009
    override suspend fun listForUser(
        userId: String,
        from: Instant?,
        to: Instant?,
        type: EventType?,
        teamId: String?,
    ): List<Event> = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        val userTeamIds = TeamMembershipsTable
            .select { TeamMembershipsTable.userId eq uid }
            .map { it[TeamMembershipsTable.teamId] }

        if (userTeamIds.isEmpty()) return@newSuspendedTransaction emptyList()

        val filteredTeamIds = if (teamId != null) {
            val tid = UUID.fromString(teamId)
            if (tid !in userTeamIds) return@newSuspendedTransaction emptyList()
            listOf(tid)
        } else userTeamIds

        val rows = EventsTable
            .join(EventTeamsTable, JoinType.INNER, EventsTable.id, EventTeamsTable.eventId)
            .join(TeamsTable, JoinType.INNER, EventTeamsTable.teamId, TeamsTable.id)
            .select {
                var cond: Op<Boolean> = EventTeamsTable.teamId inList filteredTeamIds
                from?.let { f -> cond = cond and (EventsTable.startAt greaterEq f.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                to?.let { t -> cond = cond and (EventsTable.startAt lessEq t.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                type?.let { ty -> cond = cond and (EventsTable.type eq ty.name.lowercase()) }
                cond
            }
            .orderBy(EventsTable.startAt)
            .toList()

        if (rows.isEmpty()) return@newSuspendedTransaction emptyList()

        val eventIds = rows.map { it[EventsTable.id] }.distinct()

        val restrictedEventIds = EventSubgroupsTable
            .select { EventSubgroupsTable.eventId inList eventIds }
            .map { it[EventSubgroupsTable.eventId] }.toSet()

        val allowedRestrictedEventIds: Set<UUID> = if (restrictedEventIds.isEmpty()) emptySet() else {
            val userSubgroupIds = SubgroupMembersTable
                .select { SubgroupMembersTable.userId eq uid }
                .map { it[SubgroupMembersTable.subgroupId] }.toSet()
            if (userSubgroupIds.isEmpty()) emptySet()
            else EventSubgroupsTable
                .select {
                    (EventSubgroupsTable.eventId inList restrictedEventIds) and
                    (EventSubgroupsTable.subgroupId inList userSubgroupIds)
                }
                .map { it[EventSubgroupsTable.eventId] }.toSet()
        }

        rows
            .filter { row ->
                val eid = row[EventsTable.id]
                eid !in restrictedEventIds || eid in allowedRestrictedEventIds
            }
            .groupBy { it[EventsTable.id] }
            .map { (_, groupRows) ->
                groupRows.first().toEvent(
                    matchedTeams = groupRows.map {
                        TeamRef(it[EventTeamsTable.teamId].toString(), it[TeamsTable.name])
                    }
                )
            }
    }

    // ES-010
    override suspend fun listForTeam(
        teamId: String,
        from: Instant?,
        to: Instant?,
        type: EventType?,
    ): List<Event> = newSuspendedTransaction {
        val tid = UUID.fromString(teamId)
        val rows = EventsTable
            .join(EventTeamsTable, JoinType.INNER, EventsTable.id, EventTeamsTable.eventId)
            .select {
                var cond: Op<Boolean> = EventTeamsTable.teamId eq tid
                from?.let { f -> cond = cond and (EventsTable.startAt greaterEq f.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                to?.let { t -> cond = cond and (EventsTable.startAt lessEq t.toJavaInstant().atOffset(ZoneOffset.UTC)) }
                type?.let { ty -> cond = cond and (EventsTable.type eq ty.name.lowercase()) }
                cond
            }
            .orderBy(EventsTable.startAt)
            .toList()

        val eventIds = rows.map { it[EventsTable.id] }.distinct()
        val teamsByEvent = fetchTeamsByEvents(eventIds)
        val subgroupsByEvent = fetchSubgroupsByEvents(eventIds)
        rows.distinctBy { it[EventsTable.id] }.map { row ->
            val eid = row[EventsTable.id]
            row.toEvent(teams = teamsByEvent[eid] ?: emptyList(), subgroups = subgroupsByEvent[eid] ?: emptyList())
        }
    }

    // ES-011
    override suspend fun getById(eventId: String): Event? = newSuspendedTransaction {
        val id = UUID.fromString(eventId)
        val row = EventsTable.select { EventsTable.id eq id }.singleOrNull() ?: return@newSuspendedTransaction null
        val teams = fetchTeamsByEvents(listOf(id))[id] ?: emptyList()
        val subgroups = fetchSubgroupsByEvents(listOf(id))[id] ?: emptyList()
        row.toEvent(teams = teams, subgroups = subgroups)
    }

    // ES-012 + ES-022
    override suspend fun create(request: CreateEventRequest, createdBy: String): Event =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val creatorId = UUID.fromString(createdBy)
            val eventId = UUID.randomUUID()

            val seriesId: UUID? = if (request.recurring != null) createSeries(request.recurring, creatorId, now) else null

            EventsTable.insert {
                it[id] = eventId
                it[title] = request.title
                it[type] = request.type.name.lowercase()
                it[startAt] = request.startAt.toJavaInstant().atOffset(ZoneOffset.UTC)
                it[endAt] = request.endAt.toJavaInstant().atOffset(ZoneOffset.UTC)
                it[meetupAt] = request.meetupAt?.toJavaInstant()?.atOffset(ZoneOffset.UTC)
                it[location] = request.location
                it[description] = request.description
                it[minAttendees] = request.minAttendees
                it[status] = "active"
                it[EventsTable.seriesId] = seriesId
                it[seriesSequence] = if (seriesId != null) 1 else null
                it[seriesOverride] = false
                it[EventsTable.createdBy] = creatorId
                it[createdAt] = now
                it[updatedAt] = now
            }

            val teamUUIDs = request.teamIds.map { UUID.fromString(it) }
            val subgroupUUIDs = request.subgroupIds.map { UUID.fromString(it) }
            teamUUIDs.forEach { tid -> EventTeamsTable.insertIgnore { it[EventTeamsTable.eventId] = eventId; it[EventTeamsTable.teamId] = tid } }
            subgroupUUIDs.forEach { sid -> EventSubgroupsTable.insertIgnore { it[EventSubgroupsTable.eventId] = eventId; it[EventSubgroupsTable.subgroupId] = sid } }

            if (seriesId != null) {
                val horizon = request.recurring!!.seriesStartDate.plus(DatePeriod(months = 12))
                materializeSeries(
                    seriesId = seriesId,
                    fromDate = request.recurring.seriesStartDate,
                    toDate = horizon,
                    teamIds = teamUUIDs,
                    subgroupIds = subgroupUUIDs,
                    content = EventContent(request.title, request.type.name.lowercase(), request.location, request.description, request.minAttendees),
                )
            }

            // ES-020: TODO emit event.created domain event for notifications module
            val teams = fetchTeamsByEvents(listOf(eventId))[eventId] ?: emptyList()
            val subgroups = fetchSubgroupsByEvents(listOf(eventId))[eventId] ?: emptyList()
            EventsTable.select { EventsTable.id eq eventId }.single().toEvent(teams = teams, subgroups = subgroups)
        }

    // ES-013
    override suspend fun update(eventId: String, request: UpdateEventRequest): Event =
        newSuspendedTransaction {
            val eid = UUID.fromString(eventId)
            val event = EventsTable.select { EventsTable.id eq eid }.single()
            val seriesId = event[EventsTable.seriesId]
            when (request.scope) {
                RecurringScope.THIS_ONLY -> updateThisOnly(eid, request)
                RecurringScope.THIS_AND_FUTURE -> if (seriesId == null) updateThisOnly(eid, request) else updateThisAndFuture(eid, seriesId, request, event)
                RecurringScope.ALL -> if (seriesId == null) updateThisOnly(eid, request) else updateAll(eid, seriesId, request, event)
            }
        }

    // ES-014
    override suspend fun cancel(eventId: String, request: CancelEventRequest): Event =
        newSuspendedTransaction {
            val eid = UUID.fromString(eventId)
            val event = EventsTable.select { EventsTable.id eq eid }.single()
            val seriesId = event[EventsTable.seriesId]
            val now = OffsetDateTime.now(ZoneOffset.UTC)

            when (request.scope) {
                RecurringScope.THIS_ONLY -> {
                    EventsTable.update({ EventsTable.id eq eid }) {
                        it[status] = "cancelled"; it[cancelledAt] = now; it[updatedAt] = now
                    }
                }
                RecurringScope.THIS_AND_FUTURE, RecurringScope.ALL -> {
                    val currentStartAt = event[EventsTable.startAt]
                    if (seriesId != null) {
                        EventsTable.update({
                            (EventsTable.seriesId eq seriesId) and
                            (EventsTable.startAt greaterEq currentStartAt) and
                            (EventsTable.seriesOverride eq false)
                        }) { it[status] = "cancelled"; it[cancelledAt] = now; it[updatedAt] = now }
                    }
                    // Also cancel the current event (handles override case)
                    EventsTable.update({ EventsTable.id eq eid }) {
                        it[status] = "cancelled"; it[cancelledAt] = now; it[updatedAt] = now
                    }
                }
            }

            // ES-020: TODO emit event.cancelled domain event for notifications module
            val teams = fetchTeamsByEvents(listOf(eid))[eid] ?: emptyList()
            EventsTable.select { EventsTable.id eq eid }.single().toEvent(teams = teams)
        }

    // ES-015
    override suspend fun duplicate(eventId: String): CreateEventRequest =
        newSuspendedTransaction {
            val eid = UUID.fromString(eventId)
            val event = EventsTable.select { EventsTable.id eq eid }.single()
            val teamIds = EventTeamsTable.select { EventTeamsTable.eventId eq eid }.map { it[EventTeamsTable.teamId].toString() }
            val subgroupIds = EventSubgroupsTable.select { EventSubgroupsTable.eventId eq eid }.map { it[EventSubgroupsTable.subgroupId].toString() }
            CreateEventRequest(
                title = event[EventsTable.title],
                type = event[EventsTable.type].toEventType(),
                startAt = event[EventsTable.startAt].toInstant().toKotlinInstant(),
                endAt = event[EventsTable.endAt].toInstant().toKotlinInstant(),
                meetupAt = event[EventsTable.meetupAt]?.toInstant()?.toKotlinInstant(),
                location = event[EventsTable.location],
                description = event[EventsTable.description],
                minAttendees = event[EventsTable.minAttendees],
                teamIds = teamIds,
                subgroupIds = subgroupIds,
                recurring = null,
            )
        }

    // ES-008
    override suspend fun resolveTargetedUsers(eventId: String): List<String> =
        newSuspendedTransaction {
            val eid = UUID.fromString(eventId)
            val subgroupIds = EventSubgroupsTable.select { EventSubgroupsTable.eventId eq eid }.map { it[EventSubgroupsTable.subgroupId] }

            if (subgroupIds.isEmpty()) {
                val teamIds = EventTeamsTable.select { EventTeamsTable.eventId eq eid }.map { it[EventTeamsTable.teamId] }
                TeamMembershipsTable.select { TeamMembershipsTable.teamId inList teamIds }
                    .map { it[TeamMembershipsTable.userId].toString() }.distinct()
            } else {
                val teamIds = EventTeamsTable.select { EventTeamsTable.eventId eq eid }.map { it[EventTeamsTable.teamId] }.toSet()
                SubgroupMembersTable.select { SubgroupMembersTable.subgroupId inList subgroupIds }
                    .map { it[SubgroupMembersTable.userId] }
                    .filter { userId ->
                        TeamMembershipsTable.select { (TeamMembershipsTable.userId eq userId) and (TeamMembershipsTable.teamId inList teamIds) }.count() > 0
                    }
                    .map { it.toString() }.distinct()
            }
        }

    // --- Scope helpers (called inside outer newSuspendedTransaction) ---

    private suspend fun updateThisOnly(eid: UUID, request: UpdateEventRequest): Event {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        // Fetch current meetupAt only if not being updated
        val currentMeetupAt = if (request.meetupAt == null)
            EventsTable.select { EventsTable.id eq eid }.single()[EventsTable.meetupAt]
        else request.meetupAt.toJavaInstant().atOffset(ZoneOffset.UTC)

        EventsTable.update({ EventsTable.id eq eid }) {
            request.title?.let { v -> it[title] = v }
            request.type?.let { v -> it[type] = v.name.lowercase() }
            request.startAt?.let { v -> it[startAt] = v.toJavaInstant().atOffset(ZoneOffset.UTC) }
            request.endAt?.let { v -> it[endAt] = v.toJavaInstant().atOffset(ZoneOffset.UTC) }
            it[meetupAt] = currentMeetupAt
            request.location?.let { v -> it[location] = v }
            request.description?.let { v -> it[description] = v }
            request.minAttendees?.let { v -> it[minAttendees] = v }
            it[seriesOverride] = true
            it[updatedAt] = now
        }
        updateAudience(eid, request.teamIds, request.subgroupIds)
        // ES-020: TODO emit event.updated
        return fetchEventDetail(eid)
    }

    private suspend fun updateThisAndFuture(eid: UUID, seriesId: UUID, request: UpdateEventRequest, event: ResultRow): Event {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val currentStartAt = event[EventsTable.startAt]
        val currentDate = currentStartAt.toLocalDate().toKotlinLocalDate()

        // Delete future non-override occurrences from original series (including current)
        EventsTable.deleteWhere {
            (EventsTable.seriesId eq seriesId) and
            (EventsTable.startAt greaterEq currentStartAt) and
            (EventsTable.seriesOverride eq false)
        }

        // Close original series before current date
        EventSeriesTable.update({ EventSeriesTable.id eq seriesId }) {
            it[seriesEndDate] = currentDate.minus(DatePeriod(days = 1))
        }

        val originalSeries = EventSeriesTable.select { EventSeriesTable.id eq seriesId }.single()
        val newSeriesId = UUID.randomUUID()
        val newStartTime = request.startAt?.let { toLocalTime(it) } ?: originalSeries[EventSeriesTable.templateStartTime]
        val newEndTime = request.endAt?.let { toLocalTime(it) } ?: originalSeries[EventSeriesTable.templateEndTime]
        val creatorId = event[EventsTable.createdBy]

        EventSeriesTable.insert {
            it[id] = newSeriesId
            it[patternType] = originalSeries[EventSeriesTable.patternType]
            it[weekdays] = originalSeries[EventSeriesTable.weekdays]
            it[intervalDays] = originalSeries[EventSeriesTable.intervalDays]
            it[seriesStartDate] = currentDate
            it[seriesEndDate] = originalSeries[EventSeriesTable.seriesEndDate]
            it[templateStartTime] = newStartTime
            it[templateEndTime] = newEndTime
            it[createdBy] = creatorId
            it[createdAt] = now
        }

        val teamIds = (request.teamIds ?: EventTeamsTable.select { EventTeamsTable.eventId eq eid }.map { it[EventTeamsTable.teamId].toString() }).map { UUID.fromString(it) }
        val subgroupIds = (request.subgroupIds ?: EventSubgroupsTable.select { EventSubgroupsTable.eventId eq eid }.map { it[EventSubgroupsTable.subgroupId].toString() }).map { UUID.fromString(it) }
        val newTitle = request.title ?: event[EventsTable.title]
        val newType = request.type?.name?.lowercase() ?: event[EventsTable.type]

        val newEventId = UUID.randomUUID()
        EventsTable.insert {
            it[id] = newEventId
            it[title] = newTitle
            it[type] = newType
            it[startAt] = request.startAt?.toJavaInstant()?.atOffset(ZoneOffset.UTC) ?: currentStartAt
            it[endAt] = request.endAt?.toJavaInstant()?.atOffset(ZoneOffset.UTC) ?: event[EventsTable.endAt]
            it[meetupAt] = request.meetupAt?.toJavaInstant()?.atOffset(ZoneOffset.UTC) ?: event[EventsTable.meetupAt]
            it[location] = request.location ?: event[EventsTable.location]
            it[description] = request.description ?: event[EventsTable.description]
            it[minAttendees] = request.minAttendees ?: event[EventsTable.minAttendees]
            it[status] = "active"
            it[EventsTable.seriesId] = newSeriesId
            it[seriesSequence] = 1
            it[seriesOverride] = false
            it[EventsTable.createdBy] = creatorId
            it[createdAt] = now
            it[updatedAt] = now
        }
        teamIds.forEach { tid -> EventTeamsTable.insertIgnore { it[EventTeamsTable.eventId] = newEventId; it[EventTeamsTable.teamId] = tid } }
        subgroupIds.forEach { sid -> EventSubgroupsTable.insertIgnore { it[EventSubgroupsTable.eventId] = newEventId; it[EventSubgroupsTable.subgroupId] = sid } }

        materializeSeries(
            seriesId = newSeriesId,
            fromDate = currentDate,
            toDate = currentDate.plus(DatePeriod(months = 12)),
            teamIds = teamIds,
            subgroupIds = subgroupIds,
            content = EventContent(newTitle, newType, request.location ?: event[EventsTable.location]),
        )

        // ES-020: TODO emit event.updated
        return fetchEventDetail(newEventId)
    }

    private suspend fun updateAll(eid: UUID, seriesId: UUID, request: UpdateEventRequest, event: ResultRow): Event {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val currentStartAt = event[EventsTable.startAt]
        val originalSeries = EventSeriesTable.select { EventSeriesTable.id eq seriesId }.single()
        val newStartTime = request.startAt?.let { toLocalTime(it) } ?: originalSeries[EventSeriesTable.templateStartTime]
        val newEndTime = request.endAt?.let { toLocalTime(it) } ?: originalSeries[EventSeriesTable.templateEndTime]

        EventSeriesTable.update({ EventSeriesTable.id eq seriesId }) {
            it[templateStartTime] = newStartTime
            it[templateEndTime] = newEndTime
        }

        EventsTable.update({
            (EventsTable.seriesId eq seriesId) and
            (EventsTable.startAt greaterEq currentStartAt) and
            (EventsTable.seriesOverride eq false)
        }) {
            request.title?.let { v -> it[title] = v }
            request.type?.let { v -> it[type] = v.name.lowercase() }
            request.location?.let { v -> it[location] = v }
            request.description?.let { v -> it[description] = v }
            request.minAttendees?.let { v -> it[minAttendees] = v }
            it[updatedAt] = now
        }

        if (request.teamIds != null || request.subgroupIds != null) {
            val futureEventIds = EventsTable.select {
                (EventsTable.seriesId eq seriesId) and
                (EventsTable.startAt greaterEq currentStartAt) and
                (EventsTable.seriesOverride eq false)
            }.map { it[EventsTable.id] }
            futureEventIds.forEach { futureEid -> updateAudience(futureEid, request.teamIds, request.subgroupIds) }
        }

        // Also apply to the current event (which may be an override)
        EventsTable.update({ EventsTable.id eq eid }) {
            request.title?.let { v -> it[title] = v }
            request.type?.let { v -> it[type] = v.name.lowercase() }
            request.location?.let { v -> it[location] = v }
            request.description?.let { v -> it[description] = v }
            request.minAttendees?.let { v -> it[minAttendees] = v }
            it[updatedAt] = now
        }
        updateAudience(eid, request.teamIds, request.subgroupIds)

        // ES-020: TODO emit event.updated
        return fetchEventDetail(eid)
    }

    private fun updateAudience(eid: UUID, teamIds: List<String>?, subgroupIds: List<String>?) {
        if (teamIds != null) {
            EventTeamsTable.deleteWhere { EventTeamsTable.eventId eq eid }
            teamIds.forEach { tid -> EventTeamsTable.insertIgnore { it[EventTeamsTable.eventId] = eid; it[EventTeamsTable.teamId] = UUID.fromString(tid) } }
        }
        if (subgroupIds != null) {
            EventSubgroupsTable.deleteWhere { EventSubgroupsTable.eventId eq eid }
            subgroupIds.forEach { sid -> EventSubgroupsTable.insertIgnore { it[EventSubgroupsTable.eventId] = eid; it[EventSubgroupsTable.subgroupId] = UUID.fromString(sid) } }
        }
    }

    private fun fetchEventDetail(eid: UUID): Event {
        val row = EventsTable.select { EventsTable.id eq eid }.single()
        val teams = fetchTeamsByEvents(listOf(eid))[eid] ?: emptyList()
        val subgroups = fetchSubgroupsByEvents(listOf(eid))[eid] ?: emptyList()
        return row.toEvent(teams = teams, subgroups = subgroups)
    }

    private fun fetchTeamsByEvents(eventIds: List<UUID>): Map<UUID, List<TeamRef>> {
        if (eventIds.isEmpty()) return emptyMap()
        return EventTeamsTable
            .join(TeamsTable, JoinType.INNER, EventTeamsTable.teamId, TeamsTable.id)
            .select { EventTeamsTable.eventId inList eventIds }
            .groupBy { it[EventTeamsTable.eventId] }
            .mapValues { (_, rows) -> rows.map { TeamRef(it[EventTeamsTable.teamId].toString(), it[TeamsTable.name]) } }
    }

    private fun fetchSubgroupsByEvents(eventIds: List<UUID>): Map<UUID, List<SubgroupRef>> {
        if (eventIds.isEmpty()) return emptyMap()
        return EventSubgroupsTable
            .join(SubgroupsTable, JoinType.INNER, EventSubgroupsTable.subgroupId, SubgroupsTable.id)
            .select { EventSubgroupsTable.eventId inList eventIds }
            .groupBy { it[EventSubgroupsTable.eventId] }
            .mapValues { (_, rows) -> rows.map { SubgroupRef(it[EventSubgroupsTable.subgroupId].toString(), it[SubgroupsTable.name]) } }
    }

    private fun createSeries(pattern: RecurringPattern, createdBy: UUID, now: OffsetDateTime): UUID {
        val id = UUID.randomUUID()
        EventSeriesTable.insert {
            it[EventSeriesTable.id] = id
            it[patternType] = pattern.patternType.name.lowercase()
            it[weekdays] = pattern.weekdays?.joinToString(",")
            it[intervalDays] = pattern.intervalDays
            it[seriesStartDate] = pattern.seriesStartDate
            it[seriesEndDate] = pattern.seriesEndDate
            it[templateStartTime] = pattern.templateStartTime
            it[templateEndTime] = pattern.templateEndTime
            it[EventSeriesTable.createdBy] = createdBy
            it[createdAt] = now
        }
        return id
    }

    private fun toLocalTime(instant: Instant): LocalTime {
        val jdt = instant.toJavaInstant().atOffset(ZoneOffset.UTC)
        return LocalTime(jdt.hour, jdt.minute, jdt.second)
    }

    private fun ResultRow.toEvent(
        matchedTeams: List<TeamRef> = emptyList(),
        teams: List<TeamRef> = emptyList(),
        subgroups: List<SubgroupRef> = emptyList(),
    ) = Event(
        id = this[EventsTable.id].toString(),
        title = this[EventsTable.title],
        type = this[EventsTable.type].toEventType(),
        startAt = this[EventsTable.startAt].toInstant().toKotlinInstant(),
        endAt = this[EventsTable.endAt].toInstant().toKotlinInstant(),
        meetupAt = this[EventsTable.meetupAt]?.toInstant()?.toKotlinInstant(),
        location = this[EventsTable.location],
        description = this[EventsTable.description],
        minAttendees = this[EventsTable.minAttendees],
        status = if (this[EventsTable.status] == "cancelled") EventStatus.CANCELLED else EventStatus.ACTIVE,
        cancelledAt = this[EventsTable.cancelledAt]?.toInstant()?.toKotlinInstant(),
        seriesId = this[EventsTable.seriesId]?.toString(),
        seriesSequence = this[EventsTable.seriesSequence],
        seriesOverride = this[EventsTable.seriesOverride],
        createdBy = this[EventsTable.createdBy].toString(),
        createdAt = this[EventsTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[EventsTable.updatedAt].toInstant().toKotlinInstant(),
        matchedTeams = matchedTeams,
        teams = teams,
        subgroups = subgroups,
    )

    private fun String.toEventType() = when (this) {
        "match" -> EventType.MATCH
        "other" -> EventType.OTHER
        else -> EventType.TRAINING
    }
}

private fun java.time.LocalDate.toKotlinLocalDate(): LocalDate =
    LocalDate(year, monthValue, dayOfMonth)

private fun kotlinx.datetime.LocalDate.minus(period: DatePeriod): kotlinx.datetime.LocalDate =
    this.plus(DatePeriod(days = -period.days, months = -period.months, years = -period.years))
