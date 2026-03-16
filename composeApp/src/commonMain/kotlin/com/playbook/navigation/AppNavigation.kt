package com.playbook.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.playbook.di.kmpViewModel

@Composable
fun AppNavigation(
    backStack: MutableList<Screen>,
    isLoggedIn: Boolean
) {
    val currentScreen = backStack.lastOrNull() ?: Screen.Loading
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { screen ->
        when (screen) {
            Screen.Loading -> PlaceholderScreen("Loading...")
            Screen.Login -> {
                val viewModel: LoginViewModel = kmpViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { backStack.add(Screen.Events) },
                    onNavigateToRegister = { backStack.add(Screen.Register) }
                )
            }
            Screen.Register -> {
                val viewModel: RegisterViewModel = kmpViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { backStack.add(Screen.EmptyState) },
                    onNavigateToLogin = { backStack.add(Screen.Login) }
                )
            }
            Screen.EmptyState -> {
                val viewModel: EmptyStateViewModel = kmpViewModel()
                EmptyStateScreen(
                    viewModel = viewModel,
                    onNavigateToClubSetup = { backStack.add(Screen.ClubSetup) },
                    onNavigateToInvite = { token -> backStack.add(Screen.Invite(token)) }
                )
            }
            Screen.ClubSetup -> {
                val viewModel: ClubSetupViewModel = kmpViewModel()
                ClubSetupScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onClubCreated = { _ -> backStack.add(Screen.Teams) }
                )
            }
            is Screen.TeamRoster -> {
                val viewModel: TeamRosterViewModel = kmpViewModel()
                TeamRosterScreen(
                    teamId = screen.teamId,
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onShareInvite = { }
                )
            }
            is Screen.Invite -> {
                val viewModel: InviteViewModel = kmpViewModel()
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
}
