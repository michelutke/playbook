package com.playbook.android.ui.clubedit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.UpdateClubRequest
import com.playbook.repository.ClubRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ClubEditEvent {
    data object Saved : ClubEditEvent()
}

class ClubEditViewModel(
    private val clubId: String,
    private val clubRepository: ClubRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ClubEditScreenState())
    val state: StateFlow<ClubEditScreenState> = _state.asStateFlow()

    private val _events = Channel<ClubEditEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: ClubEditAction) {
        when (action) {
            is ClubEditAction.NameChanged ->
                _state.update { it.copy(name = action.name, error = null) }
            is ClubEditAction.SportTypeChanged ->
                _state.update { it.copy(sportType = action.sportType, sportTypeDropdownExpanded = false) }
            is ClubEditAction.LocationChanged ->
                _state.update { it.copy(location = action.location) }
            is ClubEditAction.LogoSelected ->
                _state.update { it.copy(logoUri = action.uri) }
            is ClubEditAction.SportTypeDropdownToggled ->
                _state.update { it.copy(sportTypeDropdownExpanded = !it.sportTypeDropdownExpanded) }
            is ClubEditAction.Save -> save()
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val club = clubRepository.getById(clubId)
                if (club != null) {
                    _state.update {
                        it.copy(
                            name = club.name,
                            sportType = club.sportType,
                            location = club.location ?: "",
                            logoUrl = club.logoUrl,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Club not found.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load club.") }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (!current.isFormValid) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                clubRepository.update(
                    clubId,
                    UpdateClubRequest(
                        name = current.name.trim(),
                        sportType = current.sportType,
                        location = current.location.trim().takeIf { it.isNotBlank() },
                    ),
                )
                current.logoUri?.let { uri -> uploadLogo(uri) }
                _events.send(ClubEditEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save. Please try again.") }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun uploadLogo(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val contentType = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        clubRepository.uploadLogo(clubId, contentType, bytes)
    }
}
