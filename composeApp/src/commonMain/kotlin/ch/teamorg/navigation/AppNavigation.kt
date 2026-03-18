package ch.teamorg.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import ch.teamorg.ui.club.ClubSetupScreen
import ch.teamorg.ui.club.ClubSetupViewModel
import ch.teamorg.ui.emptystate.EmptyStateScreen
import ch.teamorg.ui.emptystate.EmptyStateViewModel
import ch.teamorg.ui.invite.InviteScreen
import ch.teamorg.ui.invite.InviteViewModel
import ch.teamorg.ui.login.LoginScreen
import ch.teamorg.ui.login.LoginViewModel
import ch.teamorg.ui.placeholder.PlaceholderScreen
import ch.teamorg.ui.register.RegisterScreen
import ch.teamorg.ui.register.RegisterViewModel
import ch.teamorg.ui.team.TeamRosterScreen
import ch.teamorg.ui.team.TeamRosterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.mp.KoinPlatform

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
                val viewModel: LoginViewModel = viewModel { KoinPlatform.getKoin().get() }
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { backStack.add(Screen.Events) },
                    onNavigateToRegister = { backStack.add(Screen.Register) }
                )
            }
            Screen.Register -> {
                val viewModel: RegisterViewModel = viewModel { KoinPlatform.getKoin().get() }
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { backStack.add(Screen.EmptyState) },
                    onNavigateToLogin = { backStack.add(Screen.Login) }
                )
            }
            Screen.EmptyState -> {
                val viewModel: EmptyStateViewModel = viewModel { KoinPlatform.getKoin().get() }
                EmptyStateScreen(
                    viewModel = viewModel,
                    onNavigateToClubSetup = { backStack.add(Screen.ClubSetup) },
                    onNavigateToInvite = { token -> backStack.add(Screen.Invite(token)) }
                )
            }
            Screen.ClubSetup -> {
                val viewModel: ClubSetupViewModel = viewModel { KoinPlatform.getKoin().get() }
                ClubSetupScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onClubCreated = { _ -> backStack.add(Screen.Teams) }
                )
            }
            is Screen.TeamRoster -> {
                val viewModel: TeamRosterViewModel = viewModel { KoinPlatform.getKoin().get() }
                TeamRosterScreen(
                    teamId = screen.teamId,
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onShareInvite = { }
                )
            }
            is Screen.Invite -> {
                val viewModel: InviteViewModel = viewModel { KoinPlatform.getKoin().get() }
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
