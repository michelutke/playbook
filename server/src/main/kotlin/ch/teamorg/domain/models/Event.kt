package ch.teamorg.domain.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Event(
    val id: UUID,
    val title: String,
    val type: String,
    val startAt: Instant,
    val endAt: Instant,
    val meetupAt: Instant?,
    val location: String?,
    val description: String?,
    val minAttendees: Int?,
    val status: String,
    val cancelledAt: Instant?,
    val seriesId: UUID?,
    val seriesSequence: Int?,
    val seriesOverride: Boolean,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val teamIds: List<UUID> = emptyList(),
    val subgroupIds: List<UUID> = emptyList()
)

data class EventWithTeams(
    val event: Event,
    val matchedTeams: List<MatchedTeam>
)

data class MatchedTeam(
    val id: UUID,
    val name: String
)

data class EventSeries(
    val id: UUID,
    val patternType: String,
    val weekdays: List<Short>?,
    val intervalDays: Int?,
    val seriesStartDate: LocalDate,
    val seriesEndDate: LocalDate?,
    val templateStartTime: LocalTime,
    val templateEndTime: LocalTime,
    val templateMeetupTime: LocalTime?,
    val templateTitle: String,
    val templateType: String,
    val templateLocation: String?,
    val templateDescription: String?,
    val templateMinAttendees: Int?,
    val createdBy: UUID,
    val createdAt: Instant
)

data class CreateEventRequest(
    val title: String,
    val type: String,
    val startAt: Instant,
    val endAt: Instant,
    val meetupAt: Instant?,
    val location: String?,
    val description: String?,
    val minAttendees: Int?,
    val teamIds: List<UUID>,
    val subgroupIds: List<UUID>,
    val recurring: RecurringPattern?
)

data class RecurringPattern(
    val patternType: String,
    val weekdays: List<Short>?,
    val intervalDays: Int?,
    val seriesEndDate: LocalDate?
)

data class EditEventRequest(
    val title: String?,
    val type: String?,
    val startAt: Instant?,
    val endAt: Instant?,
    val meetupAt: Instant?,
    val location: String?,
    val description: String?,
    val minAttendees: Int?,
    val teamIds: List<UUID>?,
    val subgroupIds: List<UUID>?
)

enum class RecurringScope { this_only, this_and_future, all }
