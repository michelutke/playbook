package com.playbook.ui.clubdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateTeamRequest
import com.playbook.domain.MemberRole
import com.playbook.domain.RejectTeamRequest
import com.playbook.domain.TeamStatus
import com.playbook.repository.ClubRepository
import com.playbook.repository.MembershipRepository
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ClubDashboardEvent {
    data class NavigateToTeam(val teamId: String) : ClubDashboardEvent()
    data object NavigateToEdit : ClubDashboardEvent()
    data object NavigateToInviteCoaches : ClubDashboardEvent()
}

class ClubDashboardViewModel(
    private val clubId: String,
    private val clubRepository: ClubRepository,
    private val teamRepository: TeamRepository,
    private val membershipRepository: MembershipRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClubDashboardScreenState())
    val state: StateFlow<ClubDashboardScreenState> = _state.asStateFlow()

    private val _events = Channel<ClubDashboardEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: ClubDashboardAction) {
        when (action) {
            is ClubDashboardAction.Refresh -> load()
            is ClubDashboardAction.ToggleArchivedTeams ->
                _state.update { it.copy(showArchivedTeams = !it.showArchivedTeams) }
            is ClubDashboardAction.ShowCreateTeamSheet ->
                _state.update { it.copy(showCreateTeamSheet = true) }
            is ClubDashboardAction.DismissCreateTeamSheet ->
                _state.update { it.copy(showCreateTeamSheet = false, newTeamName = "", newTeamDescription = "") }
            is ClubDashboardAction.NewTeamNameChanged ->
                _state.update { it.copy(newTeamName = action.name) }
            is ClubDashboardAction.NewTeamDescChanged ->
                _state.update { it.copy(newTeamDescription = action.desc) }
            is ClubDashboardAction.CreateTeam -> createTeam()
            is ClubDashboardAction.ApproveTeam -> approveTeam(action.teamId)
            is ClubDashboardAction.ShowRejectDialog ->
                _state.update { it.copy(pendingRejectionTeamId = action.teamId, rejectionReason = "") }
            is ClubDashboardAction.DismissRejectDialog ->
                _state.update { it.copy(pendingRejectionTeamId = null, rejectionReason = "") }
            is ClubDashboardAction.RejectionReasonChanged ->
                _state.update { it.copy(rejectionReason = action.reason) }
            is ClubDashboardAction.RejectTeam -> rejectTeam(action.teamId)
            is ClubDashboardAction.NavigateToTeam ->
                viewModelScope.launch { _events.send(ClubDashboardEvent.NavigateToTeam(action.teamId)) }
            is ClubDashboardAction.NavigateToEdit ->
                viewModelScope.launch { _events.send(ClubDashboardEvent.NavigateToEdit) }
            is ClubDashboardAction.NavigateToInviteCoaches ->
                viewModelScope.launch { _events.send(ClubDashboardEvent.NavigateToInviteCoaches) }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val clubDeferred = async { clubRepository.getById(clubId) }
                    val allTeamsDeferred = async { teamRepository.listByClub(clubId) }
                    val club = clubDeferred.await()
                    val allTeams = allTeamsDeferred.await()

                    val active = allTeams.filter { it.status == TeamStatus.ACTIVE }
                    val pending = allTeams.filter { it.status == TeamStatus.PENDING }
                    val archived = allTeams.filter { it.status == TeamStatus.ARCHIVED }

                    val activeSummaries = active.map { team ->
                        val roster = membershipRepository.getRoster(team.id)
                        TeamSummary(
                            team = team,
                            memberCount = roster.size,
                            coachAvatarUrls = roster
                                .filter { it.roles.contains(MemberRole.COACH) }
                                .take(3)
                                .map { it.avatarUrl },
                        )
                    }
                    val pendingSummaries = pending.map { team ->
                        TeamSummary(team = team, memberCount = 0, coachAvatarUrls = emptyList())
                    }
                    val archivedSummaries = archived.map { team ->
                        TeamSummary(team = team, memberCount = 0, coachAvatarUrls = emptyList())
                    }

                    _state.update {
                        it.copy(
                            club = club,
                            activeTeams = activeSummaries,
                            pendingTeams = pendingSummaries,
                            archivedTeams = archivedSummaries,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load dashboard.") }
            }
        }
    }

    private fun createTeam() {
        val current = _state.value
        if (current.newTeamName.isBlank()) return
        viewModelScope.launch {
            try {
                teamRepository.create(
                    clubId,
                    CreateTeamRequest(
                        name = current.newTeamName.trim(),
                        description = current.newTeamDescription.trim().takeIf { it.isNotBlank() },
                    ),
                )
                _state.update { it.copy(showCreateTeamSheet = false, newTeamName = "", newTeamDescription = "") }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create team.") }
            }
        }
    }

    private fun approveTeam(teamId: String) {
        viewModelScope.launch {
            try {
                teamRepository.approve(teamId)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to approve team.") }
            }
        }
    }

    private fun rejectTeam(teamId: String) {
        val reason = _state.value.rejectionReason.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            try {
                teamRepository.reject(teamId, RejectTeamRequest(reason))
                _state.update { it.copy(pendingRejectionTeamId = null, rejectionReason = "") }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to reject team.") }
            }
        }
    }
}
