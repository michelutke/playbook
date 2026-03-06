package com.playbook.ui.teamedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.UpdateTeamRequest
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TeamEditEvent {
    data object Saved : TeamEditEvent()
}

class TeamEditViewModel(
    private val teamId: String,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamEditSheetState())
    val state: StateFlow<TeamEditSheetState> = _state.asStateFlow()

    private val _events = Channel<TeamEditEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: TeamEditAction) {
        when (action) {
            is TeamEditAction.NameChanged -> _state.update { it.copy(name = action.name, error = null) }
            is TeamEditAction.DescChanged -> _state.update { it.copy(description = action.desc) }
            is TeamEditAction.Save -> save()
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val team = teamRepository.getById(teamId)
                if (team != null) {
                    _state.update { it.copy(name = team.name, description = team.description ?: "", isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Team not found.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load team.") }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.name.isBlank()) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                teamRepository.update(
                    teamId,
                    UpdateTeamRequest(
                        name = current.name.trim(),
                        description = current.description.trim().takeIf { it.isNotBlank() },
                    ),
                )
                _events.send(TeamEditEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save. Please try again.") }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
