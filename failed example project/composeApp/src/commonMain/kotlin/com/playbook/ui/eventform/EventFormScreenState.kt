package com.playbook.ui.eventform

import com.playbook.domain.EventType
import com.playbook.domain.PatternType
import com.playbook.domain.Subgroup
import com.playbook.domain.Team
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

enum class EventFormMode { CREATE, EDIT }

data class EventFormScreenState(
    val mode: EventFormMode = EventFormMode.CREATE,
    val eventId: String? = null,
    val title: String = "",
    val type: EventType = EventType.TRAINING,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val meetupTime: LocalTime? = null,
    val location: String = "",
    val description: String = "",
    val minAttendees: String = "",
    val selectedTeamIds: Set<String> = emptySet(),
    val selectedSubgroupIds: Set<String> = emptySet(),
    val availableTeams: List<Team> = emptyList(),
    val availableSubgroups: Map<String, List<Subgroup>> = emptyMap(),
    val isRecurring: Boolean = false,
    val patternType: PatternType = PatternType.WEEKLY,
    val selectedWeekdays: Set<Int> = emptySet(),
    val intervalDays: Int = 1,
    val seriesEndDate: LocalDate? = null,
    val showPatternSheet: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val titleError: String? = null,
    val timeError: String? = null,
)
