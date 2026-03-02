package com.playbook.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.playbook.android.ui.clubdashboard.ClubDashboardScreen
import com.playbook.android.ui.clubedit.ClubEditScreen
import com.playbook.android.ui.clubsetup.ClubSetupScreen
import com.playbook.android.ui.inviteaccept.InviteAcceptScreen
import com.playbook.android.ui.playerprofile.PlayerProfileScreen
import com.playbook.android.ui.teamdetail.TeamDetailScreen
import com.playbook.android.ui.teamsetup.TeamSetupScreen

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
    }
}
