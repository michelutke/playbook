package com.playbook.android.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object ClubSetup : Screen

    @Serializable
    data class ClubDashboard(val clubId: String) : Screen

    @Serializable
    data class TeamDetail(val teamId: String, val clubId: String) : Screen

    @Serializable
    data class ClubProfileEdit(val clubId: String) : Screen

    @Serializable
    data class PlayerProfile(val teamId: String, val userId: String) : Screen

    @Serializable
    data class CoachFirstTeamSetup(val clubId: String) : Screen

    @Serializable
    data class InviteAccept(val token: String) : Screen
}
