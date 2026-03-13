package com.playbook.ui.club

import com.playbook.di.KmpViewModel
import com.playbook.domain.Club
import com.playbook.repository.ClubRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClubSetupState(
    val name: String = "",
    val sportType: String = "volleyball",
    val location: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLogoUploading: Boolean = false,
    val logoUploadProgress: Float? = null,
    val clubId: String? = null,
    val logoUrl: String? = null
)

sealed class ClubSetupEvent {
    data class ClubCreated(val club: Club) : ClubSetupEvent()
    data object LogoUploaded : ClubSetupEvent()
}

class ClubSetupViewModel(
    private val clubRepository: ClubRepository
) : KmpViewModel() {

    private val _state = MutableStateFlow(ClubSetupState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClubSetupEvent>()
    val events = _events.asSharedFlow()

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun onSportTypeChange(sportType: String) {
        _state.value = _state.value.copy(sportType = sportType)
    }

    fun onLocationChange(location: String) {
        _state.value = _state.value.copy(location = location)
    }

    fun createClub() {
        val name = _state.value.name
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Club name is required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            clubRepository.createClub(
                name = name,
                sportType = _state.value.sportType,
                location = _state.value.location.ifBlank { null }
            ).fold(
                onSuccess = { club ->
                    _state.value = _state.value.copy(isLoading = false, clubId = club.id)
                    _events.emit(ClubSetupEvent.ClubCreated(club))
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to create club")
                }
            )
        }
    }

    fun uploadLogo(bytes: ByteArray, extension: String) {
        val clubId = _state.value.clubId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLogoUploading = true, error = null)
            clubRepository.uploadLogo(clubId, bytes, extension).fold(
                onSuccess = { club ->
                    _state.value = _state.value.copy(
                        isLogoUploading = false,
                        logoUrl = club.logoUrl
                    )
                    _events.emit(ClubSetupEvent.LogoUploaded)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLogoUploading = false,
                        error = e.message ?: "Failed to upload logo"
                    )
                }
            )
        }
    }
}
