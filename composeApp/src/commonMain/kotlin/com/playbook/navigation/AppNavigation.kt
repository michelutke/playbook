package com.playbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.NavHost
import androidx.navigation3.NavBackStackEntry
import com.playbook.ui.emptystate.EmptyStateScreen
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginScreen
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterScreen
import com.playbook.ui.register.RegisterViewModel
import com.playbook.ui.placeholder.PlaceholderScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
    currentBackStack: List<NavBackStackEntry<Screen>>,
    onNavigate: (Screen) -> Unit
) {
    NavHost(
        backStack = currentBackStack,
    ) { entry ->
        when (val screen = entry.destination) {
            Screen.Login -> {
                val viewModel: LoginViewModel = koinViewModel(
                    parameters = { parametersOf({ onNavigate(Screen.Events) }) }
                )
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { onNavigate(Screen.Register) }
                )
            }
            Screen.Register -> {
                val viewModel: RegisterViewModel = koinViewModel(
                    parameters = { parametersOf({ onNavigate(Screen.EmptyState) }) }
                )
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { onNavigate(Screen.Login) }
                )
            }
            Screen.EmptyState -> {
                val viewModel: EmptyStateViewModel = koinViewModel()
                EmptyStateScreen(viewModel = viewModel)
            }
            Screen.Events -> PlaceholderScreen("Events List")
            Screen.Calendar -> PlaceholderScreen("Calendar")
            Screen.Teams -> PlaceholderScreen("Teams")
            Screen.Inbox -> PlaceholderScreen("Inbox")
            Screen.Profile -> PlaceholderScreen("Profile")
        }
    }
}
