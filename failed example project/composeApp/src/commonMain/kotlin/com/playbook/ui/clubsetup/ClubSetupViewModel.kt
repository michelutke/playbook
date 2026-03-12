package com.playbook.ui.clubsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateClubRequest
import com.playbook.preferences.UserPreferences
import com.playbook.repository.ClubRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ClubSetupEvent {
    data class ClubCreated(val clubId: String) : ClubSetupEvent()
}

class ClubSetupViewModel(
    private val clubRepository: ClubRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ClubSetupScreenState())
    val state: StateFlow<ClubSetupScreenState> = _state.asStateFlow()

    private val _events = Channel<ClubSetupEvent>()
    val events = _events.receiveAsFlow()

    fun submitAction(action: ClubSetupAction) {
        when (action) {
            is ClubSetupAction.NameChanged ->
                _state.update { it.copy(name = action.name, error = null) }
            is ClubSetupAction.SportTypeChanged ->
                _state.update { it.copy(sportType = action.sportType, sportTypeDropdownExpanded = false) }
            is ClubSetupAction.LocationChanged ->
                _state.update { it.copy(location = action.location) }
            ClubSetupAction.SportTypeDropdownToggled ->
                _state.update { it.copy(sportTypeDropdownExpanded = !it.sportTypeDropdownExpanded) }
            ClubSetupAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.isFormValid) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                val club = clubRepository.create(
                    request = CreateClubRequest(
                        name = current.name.trim(),
                        sportType = current.sportType,
                        location = current.location.trim().takeIf { it.isNotBlank() },
                    ),
                    createdByUserId = "",
                )
                userPreferences.saveClubId(club.id)
                _events.send(ClubSetupEvent.ClubCreated(club.id))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create club. Please try again.") }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
