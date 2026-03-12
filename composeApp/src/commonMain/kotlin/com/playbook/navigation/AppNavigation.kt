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
import com.playbook.ui.club.ClubSetupScreen
import com.playbook.ui.club.ClubSetupViewModel
import com.playbook.ui.team.TeamRosterScreen
import com.playbook.ui.team.TeamRosterViewModel
import com.playbook.ui.invite.InviteScreen
import com.playbook.ui.invite.InviteViewModel
import com.playbook.ui.placeholder.PlaceholderScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
    currentBackStack: List<NavBackStackEntry<Screen>>,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    isLoggedIn: Boolean
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
                EmptyStateScreen(
                    viewModel = viewModel,
                    onNavigateToClubSetup = { onNavigate(Screen.ClubSetup) },
                    onNavigateToInvite = { token -> onNavigate(Screen.Invite(token)) }
                )
            }
            Screen.ClubSetup -> {
                val viewModel: ClubSetupViewModel = koinViewModel()
                ClubSetupScreen(
                    viewModel = viewModel,
                    onBack = onBack,
                    onClubCreated = { clubId -> onNavigate(Screen.Teams) }
                )
            }
            is Screen.TeamRoster -> {
                val viewModel: TeamRosterViewModel = koinViewModel()
                TeamRosterScreen(
                    teamId = screen.teamId,
                    viewModel = viewModel,
                    onBack = onBack,
                    onShareInvite = { url -> /* Handle share sheet */ }
                )
            }
            is Screen.Invite -> {
                val viewModel: InviteViewModel = koinViewModel()
                InviteScreen(
                    token = screen.token,
                    viewModel = viewModel,
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = { token -> onNavigate(Screen.Login) }, // In a real app, pass token to Login
                    onNavigateToRegister = { token -> onNavigate(Screen.Register) }, // In a real app, pass token to Register
                    onJoinSuccess = { onNavigate(Screen.Events) }
                )
            }
            Screen.Events -> PlaceholderScreen("Events List")
            Screen.Calendar -> PlaceholderScreen("Calendar")
            Screen.Teams -> PlaceholderScreen("Teams")
            Screen.Inbox -> PlaceholderScreen("Inbox")
            Screen.Profile -> PlaceholderScreen("Profile")
        }
    }
}
