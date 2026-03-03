package com.playbook.android.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Login : Screen

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

    @Serializable
    data class EventList(val teamId: String? = null, val clubId: String? = null) : Screen

    @Serializable
    data class EventCalendar(val teamId: String? = null) : Screen

    @Serializable
    data class EventDetail(val eventId: String) : Screen

    @Serializable
    data class EventForm(
        val clubId: String,
        val eventId: String? = null,
        val preselectedTeamId: String? = null,
        val editScope: String = "THIS_ONLY",
    ) : Screen

    @Serializable
    data class SubgroupMgmt(val teamId: String) : Screen

    @Serializable
    data class AttendanceList(val eventId: String) : Screen

    @Serializable
    data object MyAbsences : Screen

    @Serializable
    data class PlayerStats(val userId: String, val teamId: String? = null) : Screen

    @Serializable
    data class TeamStats(val teamId: String) : Screen

    @Serializable
    data object NotificationInbox : Screen

    @Serializable
    data object NotificationSettings : Screen

    @Serializable
    data object PushPermission : Screen
}
