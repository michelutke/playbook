package ch.teamorg.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.MatchedTeam
import ch.teamorg.preferences.UserPreferences
import ch.teamorg.repository.EventRepository
import ch.teamorg.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventListState(
    val events: List<EventWithTeams> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTeamId: String? = null,
    val selectedType: String? = null,
    val isCoach: Boolean = false,
    val teams: List<MatchedTeam> = emptyList()
)

class EventListViewModel(
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EventListState())
    val state = _state.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            eventRepository.getMyEvents(
                type = _state.value.selectedType,
                teamId = _state.value.selectedTeamId
            ).onSuccess { events ->
                val teams = events.flatMap { it.matchedTeams }.distinctBy { it.id }
                _state.update { it.copy(events = events, isLoading = false, teams = teams) }
                checkCoachRole()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
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

    fun setTeamFilter(teamId: String?) {
        _state.update { it.copy(selectedTeamId = teamId) }
        loadEvents()
    }

    fun setTypeFilter(type: String?) {
        _state.update { it.copy(selectedType = type) }
        loadEvents()
    }
}
