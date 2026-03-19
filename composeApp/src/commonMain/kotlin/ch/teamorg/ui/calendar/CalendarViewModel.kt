package ch.teamorg.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class CalendarState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val eventsByDate: Map<LocalDate, List<EventWithTeams>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDayEvents: List<EventWithTeams> = emptyList(),
    val isLoading: Boolean = false,
    val isCoach: Boolean = false
)

enum class CalendarViewMode { MONTH, WEEK }

class CalendarViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tz = TimeZone.currentSystemDefault()
            eventRepository.getMyEvents().onSuccess { events ->
                // Build date -> events map (including multi-day span)
                val byDate = mutableMapOf<LocalDate, MutableList<EventWithTeams>>()
                events.forEach { ewt ->
                    val startDate = ewt.event.startAt.toLocalDateTime(tz).date
                    val endDate = ewt.event.endAt.toLocalDateTime(tz).date
                    // Add event to every date it spans (covers multi-day events)
                    var date = startDate
                    while (date <= endDate) {
                        byDate.getOrPut(date) { mutableListOf() }.add(ewt)
                        date = date.plus(1, DateTimeUnit.DAY)
                    }
                }
                _state.update { it.copy(eventsByDate = byDate, isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setViewMode(mode: CalendarViewMode) { _state.update { it.copy(viewMode = mode) } }

    fun selectDate(date: LocalDate) {
        val events = _state.value.eventsByDate[date] ?: emptyList()
        _state.update { it.copy(selectedDate = date, selectedDayEvents = events) }
    }
}
