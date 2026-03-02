package com.playbook.android.ui.teamsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateTeamRequest
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TeamSetupEvent {
    data object Submitted : TeamSetupEvent()
}

class TeamSetupViewModel(
    private val clubId: String,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamSetupScreenState())
    val state: StateFlow<TeamSetupScreenState> = _state.asStateFlow()

    private val _events = Channel<TeamSetupEvent>()
    val events = _events.receiveAsFlow()

    fun submitAction(action: TeamSetupAction) {
        when (action) {
            is TeamSetupAction.NameChanged -> _state.update { it.copy(name = action.name, error = null) }
            is TeamSetupAction.DescChanged -> _state.update { it.copy(description = action.desc) }
            is TeamSetupAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.isFormValid) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                teamRepository.submitRequest(
                    clubId,
                    CreateTeamRequest(
                        name = current.name.trim(),
                        description = current.description.trim().takeIf { it.isNotBlank() },
                    ),
                    requestedByUserId = "TODO_user_id",
                )
                _events.send(TeamSetupEvent.Submitted)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to submit. Please try again.") }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
