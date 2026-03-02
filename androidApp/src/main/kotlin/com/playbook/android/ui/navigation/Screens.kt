package com.playbook.android.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object ClubSetup : Screen

    @Serializable
    data class ClubDashboard(val clubId: String) : Screen
}
