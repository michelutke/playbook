package com.playbook.ui.clubdashboard

sealed class ClubDashboardAction {
    data object Refresh : ClubDashboardAction()
    data object ToggleArchivedTeams : ClubDashboardAction()
    data object ShowCreateTeamSheet : ClubDashboardAction()
    data object DismissCreateTeamSheet : ClubDashboardAction()
    data class NewTeamNameChanged(val name: String) : ClubDashboardAction()
    data class NewTeamDescChanged(val desc: String) : ClubDashboardAction()
    data object CreateTeam : ClubDashboardAction()
    data class ApproveTeam(val teamId: String) : ClubDashboardAction()
    data class ShowRejectDialog(val teamId: String) : ClubDashboardAction()
    data object DismissRejectDialog : ClubDashboardAction()
    data class RejectionReasonChanged(val reason: String) : ClubDashboardAction()
    data class RejectTeam(val teamId: String) : ClubDashboardAction()
    data class NavigateToTeam(val teamId: String) : ClubDashboardAction()
    data object NavigateToEdit : ClubDashboardAction()
    data object NavigateToInviteCoaches : ClubDashboardAction()
}
