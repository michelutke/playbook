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

data class EventDetailState(
    val event: EventWithTeams? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCoach: Boolean = false
)

class EventDetailViewModel(
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailState())
    val state = _state.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            eventRepository.getEventDetail(eventId)
                .onSuccess { ewt ->
                    _state.update { it.copy(event = ewt, isLoading = false) }
                    loadCoachRole(ewt.matchedTeams)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    private fun loadCoachRole(teams: List<MatchedTeam>) {
        if (teams.isEmpty()) return
        val currentUserId = userPreferences.getUserId() ?: return
        viewModelScope.launch {
            for (team in teams) {
                teamRepository.getTeamRoster(team.id).onSuccess { roster ->
                    val member = roster.firstOrNull { it.userId == currentUserId }
                    if (member != null && member.role == "coach") {
                        _state.update { it.copy(isCoach = true) }
                        return@launch
                    }
                }
            }
        }
    }
}
