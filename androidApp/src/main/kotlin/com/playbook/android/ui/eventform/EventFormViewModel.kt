package com.playbook.android.ui.eventform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateEventRequest
import com.playbook.domain.EventType
import com.playbook.domain.PatternType
import com.playbook.domain.RecurringPattern
import com.playbook.domain.RecurringScope
import com.playbook.domain.UpdateEventRequest
import com.playbook.repository.EventRepository
import com.playbook.repository.SubgroupRepository
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

sealed class EventFormEvent {
    data object NavigateBack : EventFormEvent()
}

class EventFormViewModel(
    private val clubId: String,
    private val eventId: String?,
    private val preselectedTeamId: String?,
    private val editScope: RecurringScope,
    private val eventRepository: EventRepository,
    private val subgroupRepository: SubgroupRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val tz = TimeZone.currentSystemDefault()

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<EventFormScreenState> = _state.asStateFlow()

    private val _navEvents = Channel<EventFormEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        loadTeams()
        if (eventId != null) loadEvent(eventId)
    }

    fun submitAction(action: EventFormAction) {
        when (action) {
            is EventFormAction.TitleChanged -> _state.update { it.copy(title = action.value, titleError = null) }
            is EventFormAction.TypeSelected -> _state.update { it.copy(type = action.type) }
            is EventFormAction.StartDateChanged -> _state.update { it.copy(startDate = action.date, timeError = null) }
            is EventFormAction.StartTimeChanged -> _state.update { it.copy(startTime = action.time, timeError = null) }
            is EventFormAction.EndDateChanged -> _state.update { it.copy(endDate = action.date, timeError = null) }
            is EventFormAction.EndTimeChanged -> _state.update { it.copy(endTime = action.time, timeError = null) }
            is EventFormAction.MeetupTimeChanged -> _state.update { it.copy(meetupTime = action.time) }
            is EventFormAction.LocationChanged -> _state.update { it.copy(location = action.value) }
            is EventFormAction.DescriptionChanged -> _state.update { it.copy(description = action.value) }
            is EventFormAction.MinAttendeesChanged -> _state.update { it.copy(minAttendees = action.value) }
            is EventFormAction.TeamToggled -> toggleTeam(action.teamId)
            is EventFormAction.SubgroupToggled -> toggleSubgroup(action.subgroupId)
            is EventFormAction.RecurringToggled -> _state.update { it.copy(isRecurring = action.enabled, showPatternSheet = action.enabled) }
            is EventFormAction.PatternTypeSelected -> _state.update { it.copy(patternType = action.patternType) }
            is EventFormAction.WeekdayToggled -> toggleWeekday(action.weekday)
            is EventFormAction.IntervalDaysChanged -> _state.update { it.copy(intervalDays = action.value.toIntOrNull()?.coerceAtLeast(1) ?: it.intervalDays) }
            is EventFormAction.SeriesEndDateChanged -> _state.update { it.copy(seriesEndDate = action.date) }
            EventFormAction.PatternSheetDismissed -> _state.update { it.copy(showPatternSheet = false) }
            EventFormAction.Submit -> submit()
        }
    }

    private fun buildInitialState(): EventFormScreenState {
        val now = Clock.System.now().toLocalDateTime(tz)
        val startHour = (now.hour + 1) % 24
        val startTime = LocalTime(startHour, 0)
        val endTime = LocalTime((startHour + 1) % 24, 30)
        return EventFormScreenState(
            mode = if (eventId != null) EventFormMode.EDIT else EventFormMode.CREATE,
            eventId = eventId,
            startDate = now.date,
            startTime = startTime,
            endDate = now.date,
            endTime = endTime,
            selectedTeamIds = if (preselectedTeamId != null) setOf(preselectedTeamId) else emptySet(),
        )
    }

    private fun loadTeams() {
        viewModelScope.launch {
            try {
                val teams = teamRepository.listByClub(clubId)
                _state.update { it.copy(availableTeams = teams) }
                // Load subgroups for pre-selected team
                if (preselectedTeamId != null) loadSubgroups(preselectedTeamId)
            } catch (_: Exception) { }
        }
    }

    private fun loadEvent(id: String) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val event = eventRepository.getById(id) ?: return@launch
                val startLocal = event.startAt.toLocalDateTime(tz)
                val endLocal = event.endAt.toLocalDateTime(tz)
                val meetupLocal = event.meetupAt?.toLocalDateTime(tz)
                _state.update {
                    it.copy(
                        title = event.title,
                        type = event.type,
                        startDate = startLocal.date,
                        startTime = startLocal.time,
                        endDate = endLocal.date,
                        endTime = endLocal.time,
                        meetupTime = meetupLocal?.time,
                        location = event.location ?: "",
                        description = event.description ?: "",
                        minAttendees = event.minAttendees?.toString() ?: "",
                        selectedTeamIds = event.teams.map { t -> t.id }.toSet(),
                        selectedSubgroupIds = event.subgroups.map { s -> s.id }.toSet(),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load event.") }
            }
        }
    }

    private fun toggleTeam(teamId: String) {
        val current = _state.value.selectedTeamIds
        val updated = if (teamId in current) current - teamId else current + teamId
        _state.update { it.copy(selectedTeamIds = updated) }
        if (teamId !in current) loadSubgroups(teamId)
    }

    private fun loadSubgroups(teamId: String) {
        viewModelScope.launch {
            try {
                val subgroups = subgroupRepository.listForTeam(teamId)
                _state.update { it.copy(availableSubgroups = it.availableSubgroups + (teamId to subgroups)) }
            } catch (_: Exception) { }
        }
    }

    private fun toggleSubgroup(subgroupId: String) {
        val current = _state.value.selectedSubgroupIds
        val updated = if (subgroupId in current) current - subgroupId else current + subgroupId
        _state.update { it.copy(selectedSubgroupIds = updated) }
    }

    private fun toggleWeekday(weekday: Int) {
        val current = _state.value.selectedWeekdays
        val updated = if (weekday in current) current - weekday else current + weekday
        _state.update { it.copy(selectedWeekdays = updated) }
    }

    private fun submit() {
        val s = _state.value
        val titleError = if (s.title.isBlank()) "Title is required" else null
        val startInstant = LocalDateTime(s.startDate, s.startTime).toInstant(tz)
        val endInstant = LocalDateTime(s.endDate, s.endTime).toInstant(tz)
        val timeError = if (endInstant <= startInstant) "End time must be after start time" else null
        if (titleError != null || timeError != null) {
            _state.update { it.copy(titleError = titleError, timeError = timeError) }
            return
        }

        val meetupInstant = s.meetupTime?.let { LocalDateTime(s.startDate, it).toInstant(tz) }
        val recurring = if (s.isRecurring) {
            RecurringPattern(
                patternType = s.patternType,
                weekdays = if (s.patternType == PatternType.WEEKLY) s.selectedWeekdays.sorted() else null,
                intervalDays = if (s.patternType == PatternType.CUSTOM) s.intervalDays else null,
                seriesStartDate = s.startDate,
                seriesEndDate = s.seriesEndDate,
                templateStartTime = s.startTime,
                templateEndTime = s.endTime,
            )
        } else null

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                if (s.mode == EventFormMode.CREATE) {
                    val request = CreateEventRequest(
                        title = s.title,
                        type = s.type,
                        startAt = startInstant,
                        endAt = endInstant,
                        meetupAt = meetupInstant,
                        location = s.location.ifBlank { null },
                        description = s.description.ifBlank { null },
                        minAttendees = s.minAttendees.toIntOrNull(),
                        teamIds = s.selectedTeamIds.toList(),
                        subgroupIds = s.selectedSubgroupIds.toList(),
                        recurring = recurring,
                    )
                    eventRepository.create(request, "TODO_user_id")
                } else {
                    val request = UpdateEventRequest(
                        scope = editScope,
                        title = s.title,
                        type = s.type,
                        startAt = startInstant,
                        endAt = endInstant,
                        meetupAt = meetupInstant,
                        location = s.location.ifBlank { null },
                        description = s.description.ifBlank { null },
                        minAttendees = s.minAttendees.toIntOrNull(),
                        teamIds = s.selectedTeamIds.toList(),
                        subgroupIds = s.selectedSubgroupIds.toList(),
                    )
                    eventRepository.update(s.eventId!!, request)
                }
                _navEvents.send(EventFormEvent.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = "Failed to save event.") }
            }
        }
    }
}
