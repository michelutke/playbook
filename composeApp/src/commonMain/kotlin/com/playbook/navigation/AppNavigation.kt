package com.playbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.NavHost
import androidx.navigation3.NavBackStackEntry
import androidx.navigation3.NavHostEntry
import com.playbook.ui.placeholder.PlaceholderScreen

@Composable
fun AppNavigation(
    currentBackStack: List<NavBackStackEntry<Screen>>,
    onNavigate: (Screen) -> Unit
) {
    NavHost(
        backStack = currentBackStack,
        onNavigate = { onNavigate(it) }
    ) { entry ->
        when (val screen = entry.destination) {
            Screen.Login -> PlaceholderScreen("Login Screen")
            Screen.Register -> PlaceholderScreen("Register Screen")
            Screen.EmptyState -> PlaceholderScreen("Empty State - Join or Create a Club")
            Screen.Events -> PlaceholderScreen("Events List")
            Screen.Calendar -> PlaceholderScreen("Calendar")
            Screen.Teams -> PlaceholderScreen("Teams")
            Screen.Inbox -> PlaceholderScreen("Inbox")
            Screen.Profile -> PlaceholderScreen("Profile")
        }
    }
}
