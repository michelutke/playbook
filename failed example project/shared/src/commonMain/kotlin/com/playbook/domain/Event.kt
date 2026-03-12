package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

enum class EventType { TRAINING, MATCH, OTHER }
enum class EventStatus { ACTIVE, CANCELLED }
enum class PatternType { DAILY, WEEKLY, CUSTOM }
enum class RecurringScope { THIS_ONLY, THIS_AND_FUTURE, ALL }

@Serializable
data class TeamRef(val id: String, val name: String)

@Serializable
data class SubgroupRef(val id: String, val name: String)

@Serializable
data class Event(
    val id: String,
    val title: String,
    val type: EventType,
    val startAt: Instant,
    val endAt: Instant,
    val meetupAt: Instant? = null,
    val location: String? = null,
    val description: String? = null,
    val minAttendees: Int? = null,
    val status: EventStatus,
    val cancelledAt: Instant? = null,
    val seriesId: String? = null,
    val seriesSequence: Int? = null,
    val seriesOverride: Boolean,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val matchedTeams: List<TeamRef> = emptyList(),
    val teams: List<TeamRef> = emptyList(),
    val subgroups: List<SubgroupRef> = emptyList(),
)

@Serializable
data class RecurringPattern(
    val patternType: PatternType,
    val weekdays: List<Int>? = null,
    val intervalDays: Int? = null,
    val seriesStartDate: LocalDate,
    val seriesEndDate: LocalDate? = null,
    val templateStartTime: LocalTime,
    val templateEndTime: LocalTime,
)

@Serializable
data class CreateEventRequest(
    val title: String,
    val type: EventType,
    val startAt: Instant,
    val endAt: Instant,
    val meetupAt: Instant? = null,
    val location: String? = null,
    val description: String? = null,
    val minAttendees: Int? = null,
    val teamIds: List<String>,
    val subgroupIds: List<String> = emptyList(),
    val recurring: RecurringPattern? = null,
)

@Serializable
data class UpdateEventRequest(
    val scope: RecurringScope = RecurringScope.THIS_ONLY,
    val title: String? = null,
    val type: EventType? = null,
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    val meetupAt: Instant? = null,
    val location: String? = null,
    val description: String? = null,
    val minAttendees: Int? = null,
    val teamIds: List<String>? = null,
    val subgroupIds: List<String>? = null,
)

@Serializable
data class CancelEventRequest(
    val scope: RecurringScope = RecurringScope.THIS_ONLY,
)
