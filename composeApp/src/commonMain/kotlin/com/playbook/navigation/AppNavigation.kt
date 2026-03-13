package com.playbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.*
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
    navController: NavController<Screen>,
    isLoggedIn: Boolean
) {
    NavHost(
        navController = navController,
    ) { screen ->
        when (screen) {
            Screen.Loading -> PlaceholderScreen("Loading...")
            Screen.Login -> {
                val viewModel: LoginViewModel = koinViewModel(
                    parameters = { parametersOf({ navController.navigate(Screen.Events) }) }
                )
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { navController.navigate(Screen.Register) }
                )
            }
            Screen.Register -> {
                val viewModel: RegisterViewModel = koinViewModel(
                    parameters = { parametersOf({ navController.navigate(Screen.EmptyState) }) }
                )
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { navController.navigate(Screen.Login) }
                )
            }
            Screen.EmptyState -> {
                val viewModel: EmptyStateViewModel = koinViewModel()
                EmptyStateScreen(
                    viewModel = viewModel,
                    onNavigateToClubSetup = { navController.navigate(Screen.ClubSetup) },
                    onNavigateToInvite = { token -> navController.navigate(Screen.Invite(token)) }
                )
            }
            Screen.ClubSetup -> {
                val viewModel: ClubSetupViewModel = koinViewModel()
                ClubSetupScreen(
                    viewModel = viewModel,
                    onBack = { navController.pop() },
                    onClubCreated = { clubId -> navController.navigate(Screen.Teams) }
                )
            }
            is Screen.TeamRoster -> {
                val viewModel: TeamRosterViewModel = koinViewModel()
                TeamRosterScreen(
                    teamId = screen.teamId,
                    viewModel = viewModel,
                    onBack = { navController.pop() },
                    onShareInvite = { url -> /* Handle share sheet */ }
                )
            }
            is Screen.Invite -> {
                val viewModel: InviteViewModel = koinViewModel()
                InviteScreen(
                    token = screen.token,
                    viewModel = viewModel,
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = { token -> navController.navigate(Screen.Login) },
                    onNavigateToRegister = { token -> navController.navigate(Screen.Register) },
                    onJoinSuccess = { navController.navigate(Screen.Events) }
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