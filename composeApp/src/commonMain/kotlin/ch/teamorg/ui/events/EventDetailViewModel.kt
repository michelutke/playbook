package ch.teamorg.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.CheckInEntry
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.SubmitCheckInRequest
import ch.teamorg.domain.SubmitResponseRequest
import ch.teamorg.preferences.UserPreferences
import ch.teamorg.repository.AttendanceRepository
import ch.teamorg.repository.EventRepository
import ch.teamorg.repository.TeamRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class EventDetailState(
    val event: EventWithTeams? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCoach: Boolean = false,
    val myResponse: String? = null,
    val confirmedCount: Int = 0,
    val maybeCount: Int = 0,
    val declinedCount: Int = 0,
    val responseDeadline: Instant? = null,
    val deadlinePassed: Boolean = false,
    val checkInEntries: List<CheckInEntry> = emptyList()
)

sealed class DetailEvent {
    data object Cancelled : DetailEvent()
    data object Uncancelled : DetailEvent()
}

class EventDetailViewModel(
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val userPreferences: UserPreferences,
    private val attendanceRepository: AttendanceRepository
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
                    loadAttendance(eventId)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun loadAttendance(eventId: String) {
        viewModelScope.launch {
            // Load my response
            attendanceRepository.getMyResponse(eventId).onSuccess { response ->
                _state.update { it.copy(myResponse = response?.status) }
            }

            // Load check-in entries (includes all members + their responses)
            attendanceRepository.getCheckIn(eventId).onSuccess { entries ->
                val confirmed = entries.count { it.response?.status == "confirmed" }
                val maybe = entries.count { it.response?.status == "unsure" }
                val declined = entries.count {
                    it.response?.status == "declined" || it.response?.status == "declined-auto"
                }
                _state.update {
                    it.copy(
                        checkInEntries = entries,
                        confirmedCount = confirmed,
                        maybeCount = maybe,
                        declinedCount = declined
                    )
                }
            }
        }
    }

    fun submitResponse(status: String, reason: String?) {
        val eventId = _state.value.event?.event?.id ?: return
        // Optimistic update
        _state.update { it.copy(myResponse = status) }
        viewModelScope.launch {
            val request = SubmitResponseRequest(status = status, reason = reason)
            attendanceRepository.submitResponse(eventId, request)
                .onSuccess { response ->
                    _state.update { it.copy(myResponse = response.status) }
                    loadAttendance(eventId)
                }
                .onFailure {
                    // Revert optimistic update on failure
                    _state.update { it.copy(myResponse = null) }
                }
        }
    }

    fun submitOverride(userId: String, status: String, note: String?) {
        val eventId = _state.value.event?.event?.id ?: return
        viewModelScope.launch {
            val request = SubmitCheckInRequest(status = status, note = note)
            attendanceRepository.submitCheckIn(eventId, userId, request)
                .onSuccess {
                    loadAttendance(eventId)
                }
        }
    }

    fun cancelEvent(scope: String = "this_only") {
        val eventId = _state.value.event?.event?.id ?: return
        viewModelScope.launch {
            eventRepository.cancelEvent(eventId, scope)
                .onSuccess {
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
