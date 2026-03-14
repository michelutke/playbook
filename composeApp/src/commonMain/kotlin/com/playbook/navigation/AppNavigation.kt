package com.playbook.navigation

import androidx.compose.runtime.Composable
import com.playbook.ui.club.ClubSetupScreen
import com.playbook.ui.club.ClubSetupViewModel
import com.playbook.ui.emptystate.EmptyStateScreen
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.invite.InviteScreen
import com.playbook.ui.invite.InviteViewModel
import com.playbook.ui.login.LoginScreen
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.placeholder.PlaceholderScreen
import com.playbook.ui.register.RegisterScreen
import com.playbook.ui.register.RegisterViewModel
import com.playbook.ui.team.TeamRosterScreen
import com.playbook.ui.team.TeamRosterViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Platform-specific navigation shell.
 * Android: uses NavDisplay from navigation3-ui.
 * iOS: renders the current (top) screen directly.
 */
@Composable
expect fun AppNavigation(
    backStack: MutableList<Screen>,
    isLoggedIn: Boolean
)

/**
 * Shared screen content — all screen/ViewModel wiring lives here so
 * both platforms can re-use it without duplicating code.
 */
@Composable
fun ScreenContent(
    screen: Screen,
    backStack: MutableList<Screen>,
    isLoggedIn: Boolean
) {
    when (screen) {
        Screen.Loading -> PlaceholderScreen("Loading...")
        Screen.Login -> {
            val viewModel: LoginViewModel = koinViewModel(
                parameters = { parametersOf({ backStack.add(Screen.Events) }) }
            )
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { backStack.add(Screen.Register) }
            )
        }
        Screen.Register -> {
            val viewModel: RegisterViewModel = koinViewModel(
                parameters = { parametersOf({ backStack.add(Screen.EmptyState) }) }
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { backStack.add(Screen.Login) }
            )
        }
        Screen.EmptyState -> {
            val viewModel: EmptyStateViewModel = koinViewModel()
            EmptyStateScreen(
                viewModel = viewModel,
                onNavigateToClubSetup = { backStack.add(Screen.ClubSetup) },
                onNavigateToInvite = { token -> backStack.add(Screen.Invite(token)) }
            )
        }
        Screen.ClubSetup -> {
            val viewModel: ClubSetupViewModel = koinViewModel()
            ClubSetupScreen(
                viewModel = viewModel,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                onClubCreated = { _ -> backStack.add(Screen.Teams) }
            )
        }
        is Screen.TeamRoster -> {
            val viewModel: TeamRosterViewModel = koinViewModel()
            TeamRosterScreen(
                teamId = screen.teamId,
                viewModel = viewModel,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                onShareInvite = { }
            )
        }
        is Screen.Invite -> {
            val viewModel: InviteViewModel = koinViewModel()
            InviteScreen(
                token = screen.token,
                viewModel = viewModel,
                isLoggedIn = isLoggedIn,
                onNavigateToLogin = { backStack.add(Screen.Login) },
                onNavigateToRegister = { backStack.add(Screen.Register) },
                onJoinSuccess = { backStack.add(Screen.Events) }
            )
        }
        Screen.Events -> PlaceholderScreen("Events List")
        Screen.Calendar -> PlaceholderScreen("Calendar")
        Screen.Teams -> PlaceholderScreen("Teams")
        Screen.Inbox -> PlaceholderScreen("Inbox")
        Screen.Profile -> PlaceholderScreen("Profile")
    }
}
