package com.playbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
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
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeAt(backStack.lastIndex) },
        entryProvider = { screen ->
            when (screen) {
                Screen.Loading -> NavEntry(screen) {
                    PlaceholderScreen("Loading...")
                }
                Screen.Login -> NavEntry(screen) {
                    val viewModel: LoginViewModel = kmpViewModel()
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { backStack.add(Screen.Events) },
                        onNavigateToRegister = { backStack.add(Screen.Register) }
                    )
                }
                Screen.Register -> NavEntry(screen) {
                    val viewModel: RegisterViewModel = kmpViewModel()
                    RegisterScreen(
                        viewModel = viewModel,
                        onRegisterSuccess = { backStack.add(Screen.EmptyState) },
                        onNavigateToLogin = { backStack.add(Screen.Login) }
                    )
                }
                Screen.EmptyState -> NavEntry(screen) {
                    val viewModel: EmptyStateViewModel = kmpViewModel()
                    EmptyStateScreen(
                        viewModel = viewModel,
                        onNavigateToClubSetup = { backStack.add(Screen.ClubSetup) },
                        onNavigateToInvite = { token -> backStack.add(Screen.Invite(token)) }
                    )
                }
                Screen.ClubSetup -> NavEntry(screen) {
                    val viewModel: ClubSetupViewModel = kmpViewModel()
                    ClubSetupScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onClubCreated = { _ -> backStack.add(Screen.Teams) }
                    )
                }
                is Screen.TeamRoster -> NavEntry(screen) {
                    val viewModel: TeamRosterViewModel = kmpViewModel()
                    TeamRosterScreen(
                        teamId = screen.teamId,
                        viewModel = viewModel,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onShareInvite = { }
                    )
                }
                is Screen.Invite -> NavEntry(screen) {
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
                Screen.Events -> NavEntry(screen) { PlaceholderScreen("Events List") }
                Screen.Calendar -> NavEntry(screen) { PlaceholderScreen("Calendar") }
                Screen.Teams -> NavEntry(screen) { PlaceholderScreen("Teams") }
                Screen.Inbox -> NavEntry(screen) { PlaceholderScreen("Inbox") }
                Screen.Profile -> NavEntry(screen) { PlaceholderScreen("Profile") }
            }
        }
    )
}
