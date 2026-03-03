package com.playbook.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.playbook.android.ui.absences.MyAbsencesScreen
import com.playbook.android.ui.attendance.AttendanceListScreen
import com.playbook.android.ui.clubdashboard.ClubDashboardScreen
import com.playbook.android.ui.clubedit.ClubEditScreen
import com.playbook.android.ui.clubsetup.ClubSetupScreen
import com.playbook.android.ui.eventcalendar.EventCalendarScreen
import com.playbook.android.ui.eventdetail.EventDetailScreen
import com.playbook.android.ui.eventform.EventFormScreen
import com.playbook.android.ui.eventlist.EventListScreen
import com.playbook.android.ui.inviteaccept.InviteAcceptScreen
import com.playbook.android.ui.playerprofile.PlayerProfileScreen
import com.playbook.android.ui.stats.PlayerStatsScreen
import com.playbook.android.ui.stats.TeamStatsScreen
import com.playbook.android.ui.subgroupmgmt.SubgroupMgmtScreen
import com.playbook.android.ui.teamdetail.TeamDetailScreen
import com.playbook.android.ui.teamsetup.TeamSetupScreen
import com.playbook.domain.RecurringScope

@Composable
fun PlaybookNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.ClubSetup) {
        composable<Screen.ClubSetup> {
            ClubSetupScreen(
                onClubCreated = { clubId ->
                    navController.navigate(Screen.ClubDashboard(clubId))
                },
            )
        }

        composable<Screen.ClubDashboard> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.ClubDashboard>()
            ClubDashboardScreen(
                clubId = screen.clubId,
                onNavigateToTeam = { teamId ->
                    navController.navigate(Screen.TeamDetail(teamId, screen.clubId))
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.ClubProfileEdit(screen.clubId))
                },
                onNavigateToInviteCoaches = {
                    // Coach invite sheet handled inline or as a separate nav destination
                },
            )
        }

        composable<Screen.TeamDetail> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.TeamDetail>()
            TeamDetailScreen(
                teamId = screen.teamId,
                clubId = screen.clubId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEvents = {
                    navController.navigate(Screen.EventList(teamId = screen.teamId, clubId = screen.clubId))
                },
                onNavigateToSubgroups = {
                    navController.navigate(Screen.SubgroupMgmt(screen.teamId))
                },
            )
        }

        composable<Screen.ClubProfileEdit> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.ClubProfileEdit>()
            ClubEditScreen(
                clubId = screen.clubId,
                onSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.PlayerProfile> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.PlayerProfile>()
            PlayerProfileScreen(
                teamId = screen.teamId,
                userId = screen.userId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.CoachFirstTeamSetup> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.CoachFirstTeamSetup>()
            TeamSetupScreen(
                clubId = screen.clubId,
                onSubmitted = { navController.popBackStack() },
            )
        }

        composable<Screen.InviteAccept>(
            deepLinks = listOf(navDeepLink<Screen.InviteAccept>(basePath = "playbook://join")),
        ) { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.InviteAccept>()
            InviteAcceptScreen(
                token = screen.token,
                onAccepted = { clubId ->
                    navController.navigate(Screen.ClubDashboard(clubId)) {
                        popUpTo(Screen.ClubSetup) { inclusive = false }
                    }
                },
                onDeclined = { navController.popBackStack() },
            )
        }

        composable<Screen.EventList> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.EventList>()
            EventListScreen(
                teamId = screen.teamId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { eventId ->
                    navController.navigate(Screen.EventDetail(eventId))
                },
                onNavigateToCreate = { teamId ->
                    val clubId = screen.clubId ?: return@EventListScreen
                    navController.navigate(Screen.EventForm(clubId = clubId, preselectedTeamId = teamId))
                },
                onNavigateToCalendar = { teamId ->
                    navController.navigate(Screen.EventCalendar(teamId))
                },
            )
        }

        composable<Screen.EventCalendar> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.EventCalendar>()
            EventCalendarScreen(
                teamId = screen.teamId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { eventId ->
                    navController.navigate(Screen.EventDetail(eventId))
                },
                onNavigateToList = { teamId ->
                    navController.popBackStack()
                },
            )
        }

        composable<Screen.EventDetail> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.EventDetail>()
            EventDetailScreen(
                eventId = screen.eventId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { eventId, scope ->
                    navController.navigate(
                        Screen.EventForm(
                            clubId = "TODO_clubId",
                            eventId = eventId,
                            editScope = scope.name,
                        )
                    )
                },
                onNavigateToDuplicate = { sourceEventId ->
                    navController.navigate(Screen.EventForm(clubId = "TODO_clubId", eventId = null))
                },
            )
        }

        composable<Screen.EventForm> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.EventForm>()
            EventFormScreen(
                clubId = screen.clubId,
                eventId = screen.eventId,
                preselectedTeamId = screen.preselectedTeamId,
                editScope = RecurringScope.valueOf(screen.editScope),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.SubgroupMgmt> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.SubgroupMgmt>()
            SubgroupMgmtScreen(
                teamId = screen.teamId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.AttendanceList> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.AttendanceList>()
            AttendanceListScreen(
                eventId = screen.eventId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.MyAbsences> {
            MyAbsencesScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.PlayerStats> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.PlayerStats>()
            PlayerStatsScreen(
                userId = screen.userId,
                teamId = screen.teamId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.TeamStats> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.TeamStats>()
            TeamStatsScreen(
                teamId = screen.teamId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayerStats = { userId ->
                    navController.navigate(Screen.PlayerStats(userId = userId, teamId = screen.teamId))
                },
            )
        }
    }
}
