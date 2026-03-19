package ch.teamorg.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import ch.teamorg.ui.club.ClubSetupScreen
import ch.teamorg.ui.club.ClubSetupViewModel
import ch.teamorg.ui.emptystate.EmptyStateScreen
import ch.teamorg.ui.emptystate.EmptyStateViewModel
import ch.teamorg.ui.calendar.CalendarScreen
import ch.teamorg.ui.calendar.CalendarViewModel
import ch.teamorg.ui.events.CreateEditEventScreen
import ch.teamorg.ui.events.CreateEditEventViewModel
import ch.teamorg.ui.events.EventDetailScreen
import ch.teamorg.ui.events.EventDetailViewModel
import ch.teamorg.ui.events.EventListScreen
import ch.teamorg.ui.events.EventListViewModel
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
    var detailRefreshTrigger by remember { mutableIntStateOf(0) }
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
            Screen.Events -> {
                val viewModel: EventListViewModel = viewModel { KoinPlatform.getKoin().get() }
                EventListScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId -> backStack.add(Screen.EventDetail(eventId)) },
                    onCreateClick = { backStack.add(Screen.CreateEvent) }
                )
            }
            is Screen.EventDetail -> {
                val viewModel: EventDetailViewModel = viewModel { KoinPlatform.getKoin().get() }
                LaunchedEffect(screen.eventId, detailRefreshTrigger) {
                    viewModel.loadEvent(screen.eventId)
                }
                EventDetailScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onEdit = { backStack.add(Screen.EditEvent(screen.eventId)) },
                    onDuplicate = { backStack.add(Screen.CreateEvent) },
                    onCancel = {
                        detailRefreshTrigger++
                        backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
            Screen.CreateEvent -> {
                val viewModel: CreateEditEventViewModel = viewModel { KoinPlatform.getKoin().get() }
                CreateEditEventScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onSaved = { backStack.removeAt(backStack.lastIndex) }
                )
            }
            is Screen.EditEvent -> {
                val viewModel: CreateEditEventViewModel = viewModel { KoinPlatform.getKoin().get() }
                LaunchedEffect(screen.eventId) { viewModel.loadForEdit(screen.eventId) }
                CreateEditEventScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onSaved = {
                        detailRefreshTrigger++
                        backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
            Screen.Calendar -> {
                val viewModel: CalendarViewModel = viewModel { KoinPlatform.getKoin().get() }
                CalendarScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId -> backStack.add(Screen.EventDetail(eventId)) },
                    onCreateClick = { backStack.add(Screen.CreateEvent) }
                )
            }
            Screen.Teams -> PlaceholderScreen("Teams")
            Screen.Inbox -> PlaceholderScreen("Inbox")
            Screen.Profile -> PlaceholderScreen("Profile")
        }
    }
}
