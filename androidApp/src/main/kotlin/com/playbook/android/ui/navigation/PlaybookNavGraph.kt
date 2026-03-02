package com.playbook.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.playbook.android.ui.clubsetup.ClubSetupScreen

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
        composable<Screen.ClubDashboard> {
            // TM-053: Club Dashboard
        }
    }
}
