package com.playbook.ui.clubdashboard

import com.playbook.domain.Club
import com.playbook.domain.Team

data class TeamSummary(
    val team: Team,
    val memberCount: Int,
    val coachAvatarUrls: List<String?>,
)

data class ClubDashboardScreenState(
    val club: Club? = null,
    val activeTeams: List<TeamSummary> = emptyList(),
    val pendingTeams: List<TeamSummary> = emptyList(),
    val archivedTeams: List<TeamSummary> = emptyList(),
    val showArchivedTeams: Boolean = false,
    val showCreateTeamSheet: Boolean = false,
    val newTeamName: String = "",
    val newTeamDescription: String = "",
    val pendingRejectionTeamId: String? = null,
    val rejectionReason: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)
