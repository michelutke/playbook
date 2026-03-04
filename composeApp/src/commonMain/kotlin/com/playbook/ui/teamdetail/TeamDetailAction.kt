package com.playbook.ui.teamdetail

sealed class TeamDetailAction {
    data object Refresh : TeamDetailAction()
    data class SearchQueryChanged(val query: String) : TeamDetailAction()
    data class TabSelected(val tab: TeamDetailTab) : TeamDetailAction()
    data class RemoveMember(val userId: String) : TeamDetailAction()
    data object LeaveTeam : TeamDetailAction()
}
