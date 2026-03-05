package com.playbook.ui.eventcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.Event
import com.playbook.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

sealed class EventCalendarEvent {
    data class NavigateToDetail(val eventId: String) : EventCalendarEvent()
    data object NavigateToList : EventCalendarEvent()
}

class EventCalendarViewModel(
    private val teamId: String?,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val today: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val _state = MutableStateFlow(
        EventCalendarScreenState(
            focusedMonthStart = LocalDate(today.year, today.monthNumber, 1),
            focusedWeekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal)),
        )
    )
    val state: StateFlow<EventCalendarScreenState> = _state.asStateFlow()

    private val _events = Channel<EventCalendarEvent>()
    val events = _events.receiveAsFlow()

    private var allEvents: List<Event> = emptyList()

    init {
        load()
    }

    fun submitAction(action: EventCalendarAction) {
        when (action) {
            EventCalendarAction.Refresh -> load()
            is EventCalendarAction.ViewToggled -> _state.update { it.copy(view = action.view, selectedDate = null) }
            is EventCalendarAction.DateSelected -> _state.update {
                it.copy(selectedDate = if (it.selectedDate == action.date) null else action.date)
            }
            is EventCalendarAction.MonthNavigated -> navigateMonth(action.forward)
            is EventCalendarAction.WeekNavigated -> navigateWeek(action.forward)
            is EventCalendarAction.EventSelected ->
                viewModelScope.launch { _events.send(EventCalendarEvent.NavigateToDetail(action.eventId)) }
            EventCalendarAction.OpenList ->
                viewModelScope.launch { _events.send(EventCalendarEvent.NavigateToList) }
        }
    }

    private fun navigateMonth(forward: Boolean) {
        val current = _state.value.focusedMonthStart
        val next = if (forward) {
            if (current.monthNumber == 12) LocalDate(current.year + 1, 1, 1)
            else LocalDate(current.year, current.monthNumber + 1, 1)
        } else {
            if (current.monthNumber == 1) LocalDate(current.year - 1, 12, 1)
            else LocalDate(current.year, current.monthNumber - 1, 1)
        }
        _state.update { it.copy(focusedMonthStart = next, selectedDate = null) }
        load()
    }

    private fun navigateWeek(forward: Boolean) {
        val current = _state.value.focusedWeekStart
        val next = if (forward) current.plus(DatePeriod(days = 7)) else current.minus(DatePeriod(days = 7))
        _state.update { it.copy(focusedWeekStart = next, selectedDate = null) }
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val loaded = if (teamId != null) {
                    eventRepository.listForTeam(teamId)
                } else {
                    eventRepository.listForUser("TODO_user_id")
                }
                allEvents = loaded
                _state.update { it.copy(events = loaded, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load events.") }
            }
        }
    }
}
