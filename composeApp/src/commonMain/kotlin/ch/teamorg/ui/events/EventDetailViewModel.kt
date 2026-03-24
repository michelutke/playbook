package ch.teamorg.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.preferences.UserPreferences
import ch.teamorg.repository.EventRepository
import ch.teamorg.repository.TeamRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailState(
    val event: EventWithTeams? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCoach: Boolean = false
)

sealed class DetailEvent {
    data object Cancelled : DetailEvent()
    data object Uncancelled : DetailEvent()
}

class EventDetailViewModel(
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>()
    val events = _events.asSharedFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            eventRepository.getEventDetail(eventId)
                .onSuccess { ewt ->
                    _state.update { it.copy(event = ewt, isLoading = false) }
                    checkCoachRole()
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun cancelEvent(scope: String = "this_only") {
        val eventId = _state.value.event?.event?.id ?: return
        viewModelScope.launch {
            eventRepository.cancelEvent(eventId, scope)
                .onSuccess {
                    // Reload to reflect cancelled state
                    loadEvent(eventId)
                    _events.emit(DetailEvent.Cancelled)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    fun uncancelEvent(scope: String = "this_only") {
        val eventId = _state.value.event?.event?.id ?: return
        viewModelScope.launch {
            eventRepository.uncancelEvent(eventId, scope)
                .onSuccess {
                    loadEvent(eventId)
                    _events.emit(DetailEvent.Uncancelled)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun checkCoachRole() {
        viewModelScope.launch {
            teamRepository.getMyRoles().onSuccess { roles ->
                val isCoachOrManager = roles.teamRoles.any { it.role == "coach" } ||
                    roles.clubRoles.any { it.role == "club_manager" }
                _state.update { it.copy(isCoach = isCoachOrManager) }
            }
        }
    }
}
