package ch.teamorg.data

import ch.teamorg.db.TeamorgDb
import ch.teamorg.domain.Event
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.MatchedTeam
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventCacheManager(private val database: TeamorgDb) {

    private val queries = database.eventQueries

    fun saveEvents(events: List<EventWithTeams>) {
        val now = Clock.System.now().toEpochMilliseconds()
        events.forEach { ewt ->
            val e = ewt.event
            queries.upsertEvent(
                id = e.id,
                title = e.title,
                type = e.type,
                start_at = e.startAt.toEpochMilliseconds(),
                end_at = e.endAt.toEpochMilliseconds(),
                meetup_at = e.meetupAt?.toEpochMilliseconds(),
                location = e.location,
                description = e.description,
                min_attendees = e.minAttendees?.toLong(),
                status = e.status,
                cancelled_at = e.cancelledAt?.toEpochMilliseconds(),
                series_id = e.seriesId,
                series_sequence = e.seriesSequence?.toLong(),
                series_override = if (e.seriesOverride) 1L else 0L,
                created_by = e.createdBy,
                team_ids = e.teamIds.joinToString(","),
                subgroup_ids = e.subgroupIds.joinToString(","),
                matched_teams_json = Json.encodeToString(ewt.matchedTeams),
                cached_at = now
            )
        }
    }

    fun getOfflineEvents(fromMillis: Long, toMillis: Long): List<EventWithTeams> {
        return queries.getUpcomingEvents(fromMillis, toMillis).executeAsList().map { row ->
            rowToEventWithTeams(row)
        }
    }

    fun getFilteredOfflineEvents(fromMillis: Long, toMillis: Long, type: String?, teamId: String?): List<EventWithTeams> {
        return queries.getFilteredEvents(fromMillis, toMillis, type, teamId).executeAsList().map { row ->
            rowToEventWithTeams(row)
        }
    }

    fun cleanup() {
        val cutoff = Clock.System.now()
            .minus(7, DateTimeUnit.DAY, TimeZone.UTC)
            .toEpochMilliseconds()
        queries.deleteOlderThan(cutoff)
    }

    private fun rowToEventWithTeams(row: ch.teamorg.CachedEvent): EventWithTeams {
        val event = Event(
            id = row.id,
            title = row.title,
            type = row.type,
            startAt = Instant.fromEpochMilliseconds(row.start_at),
            endAt = Instant.fromEpochMilliseconds(row.end_at),
            meetupAt = row.meetup_at?.let { Instant.fromEpochMilliseconds(it) },
            location = row.location,
            description = row.description,
            minAttendees = row.min_attendees?.toInt(),
            status = row.status,
            cancelledAt = row.cancelled_at?.let { Instant.fromEpochMilliseconds(it) },
            seriesId = row.series_id,
            seriesSequence = row.series_sequence?.toInt(),
            seriesOverride = row.series_override != 0L,
            createdBy = row.created_by,
            createdAt = Instant.fromEpochMilliseconds(row.cached_at),
            updatedAt = Instant.fromEpochMilliseconds(row.cached_at),
            teamIds = if (row.team_ids.isEmpty()) emptyList() else row.team_ids.split(","),
            subgroupIds = if (row.subgroup_ids.isEmpty()) emptyList() else row.subgroup_ids.split(",")
        )
        val matchedTeams: List<MatchedTeam> = try {
            Json.decodeFromString(row.matched_teams_json)
        } catch (_: Exception) {
            emptyList()
        }
        return EventWithTeams(event = event, matchedTeams = matchedTeams)
    }
}
