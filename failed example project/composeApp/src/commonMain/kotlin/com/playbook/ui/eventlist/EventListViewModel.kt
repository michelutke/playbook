package com.playbook.ui.eventlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.Event
import com.playbook.domain.EventType
import com.playbook.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EventListEvent {
    data class NavigateToDetail(val eventId: String) : EventListEvent()
    data object NavigateToCreate : EventListEvent()
    data object NavigateToCalendar : EventListEvent()
}

class EventListViewModel(
    private val teamId: String?,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventListScreenState())
    val state: StateFlow<EventListScreenState> = _state.asStateFlow()

    private val _events = Channel<EventListEvent>()
    val events = _events.receiveAsFlow()

    private var allEvents: List<Event> = emptyList()

    init {
        load()
    }

    fun submitAction(action: EventListAction) {
        when (action) {
            EventListAction.Refresh -> load()
            is EventListAction.TypeFilterChanged -> {
                _state.update { it.copy(selectedType = action.type) }
                applyFilter()
            }
            is EventListAction.TeamFilterChanged -> {
                _state.update { it.copy(selectedTeamId = action.teamId) }
                applyFilter()
            }
            is EventListAction.EventSelected ->
                viewModelScope.launch { _events.send(EventListEvent.NavigateToDetail(action.eventId)) }
            EventListAction.CreateEvent ->
                viewModelScope.launch { _events.send(EventListEvent.NavigateToCreate) }
            EventListAction.OpenCalendar ->
                viewModelScope.launch { _events.send(EventListEvent.NavigateToCalendar) }
        }
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
                val teams = loaded.flatMap { it.teams }.distinctBy { it.id }
                _state.update {
                    it.copy(
                        events = applyFilters(loaded, it.selectedType, it.selectedTeamId),
                        availableTeams = teams,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load events.") }
            }
        }
    }

    private fun applyFilter() {
        val s = _state.value
        _state.update { it.copy(events = applyFilters(allEvents, s.selectedType, s.selectedTeamId)) }
    }

    private fun applyFilters(events: List<Event>, type: EventType?, teamId: String?): List<Event> =
        events
            .filter { type == null || it.type == type }
            .filter { teamId == null || it.teams.any { t -> t.id == teamId } }
            .sortedBy { it.startAt }
}
