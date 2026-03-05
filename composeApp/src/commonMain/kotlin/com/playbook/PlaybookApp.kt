package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.playbook.auth.AuthState
import com.playbook.auth.AuthViewModel
import com.playbook.ui.clubcoachinvite.ClubCoachInviteSheet
import com.playbook.ui.clubdashboard.ClubDashboardScreen
import com.playbook.ui.clubedit.ClubEditScreen
import com.playbook.ui.clubsetup.ClubSetupScreen
import com.playbook.ui.components.PlaybookBottomBar
import com.playbook.ui.inviteaccept.InviteAcceptScreen
import com.playbook.ui.login.LoginScreen
import com.playbook.ui.playerprofile.PlayerProfileScreen
import com.playbook.ui.stats.PlayerStatsScreen
import com.playbook.ui.stats.TeamStatsScreen
import com.playbook.domain.RecurringScope
import com.playbook.ui.absences.MyAbsencesScreen
import com.playbook.ui.attendancelist.AttendanceListScreen
import com.playbook.ui.eventcalendar.EventCalendarScreen
import com.playbook.ui.eventdetail.EventDetailScreen
import com.playbook.ui.eventform.EventFormScreen
import com.playbook.ui.eventlist.EventListScreen
import com.playbook.ui.subgroupmgmt.SubgroupMgmtScreen
import com.playbook.ui.teamdetail.TeamDetailScreen
import com.playbook.ui.teamedit.TeamEditSheet
import com.playbook.ui.teaminvite.TeamInviteSheet
import com.playbook.ui.teamsetup.TeamSetupScreen
import kotlinx.coroutines.flow.filter
import org.koin.compose.koinInject

@Composable
fun PlaybookApp(deepLinkToken: String? = null) {
    val authViewModel: AuthViewModel = koinInject()
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val currentScreen = backStack.lastOrNull()
    var coachInviteClubId by remember { mutableStateOf<String?>(null) }
    var teamEditTeamId by remember { mutableStateOf<String?>(null) }
    var teamInviteTeamId by remember { mutableStateOf<String?>(null) }

    // Auth-driven routing: initial cold start + logout
    LaunchedEffect(Unit) {
        authViewModel.authState
            .filter { it !is AuthState.Loading }
            .collect { state ->
                backStack.clear()
                when (state) {
                    is AuthState.Unauthenticated -> backStack.add(Screen.Login)
                    is AuthState.Authenticated -> {
                        if (state.clubId != null) backStack.add(Screen.ClubDashboard(state.clubId))
                        else backStack.add(Screen.ClubSetup)
                    }
                    else -> backStack.add(Screen.Login)
                }
            }
    }

    // Deep link: navigate to InviteAccept when authenticated
    LaunchedEffect(deepLinkToken) {
        if (deepLinkToken == null) return@LaunchedEffect
        val state = authViewModel.authState.value
        if (state is AuthState.Authenticated) {
            backStack.add(Screen.InviteAccept(deepLinkToken))
        }
    }

    val showBottomBar = currentScreen is Screen.ClubDashboard ||
        currentScreen is Screen.NotificationInbox

    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    PlaybookBottomBar(
                        isHomeSelected = currentScreen is Screen.ClubDashboard,
                        isNotificationsSelected = currentScreen is Screen.NotificationInbox,
                        unreadCount = 0,
                        onHomeClick = {
                            val idx = backStack.indexOfFirst { it is Screen.ClubDashboard }
                            if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
                        },
                        onNotificationsClick = {
                            if (currentScreen !is Screen.NotificationInbox) {
                                backStack.add(Screen.NotificationInbox)
                            }
                        },
                    )
                }
            }
        ) { padding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(padding).fillMaxSize(),
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryProvider = { key ->
                    NavEntry(key) {
                        when (val screen = key) {
                            Screen.Splash -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) { /* Splash — awaiting auth state */ }

                            Screen.Login, Screen.Register -> LoginScreen()

                            Screen.ClubSetup -> ClubSetupScreen(
                                onClubCreated = { clubId ->
                                    authViewModel.onLoginSuccess(clubId)
                                }
                            )

                            is Screen.CoachFirstTeamSetup -> TeamSetupScreen(
                                clubId = screen.clubId,
                                onSubmitted = { backStack.removeLastOrNull() },
                            )

                            is Screen.InviteAccept -> InviteAcceptScreen(
                                token = screen.token,
                                onAccepted = { clubId ->
                                    backStack.clear()
                                    backStack.add(Screen.ClubDashboard(clubId))
                                },
                                onDeclined = { backStack.removeLastOrNull() },
                            )

                            is Screen.ClubDashboard -> ClubDashboardScreen(
                                clubId = screen.clubId,
                                onNavigateToTeam = { teamId ->
                                    backStack.add(Screen.TeamDetail(teamId = teamId, clubId = screen.clubId))
                                },
                                onNavigateToEdit = { backStack.add(Screen.ClubProfileEdit(screen.clubId)) },
                                onNavigateToInviteCoaches = { coachInviteClubId = screen.clubId },
                            )

                            is Screen.TeamDetail -> TeamDetailScreen(
                                teamId = screen.teamId,
                                clubId = screen.clubId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToEvents = {
                                    backStack.add(Screen.EventList(teamId = screen.teamId))
                                },
                                onNavigateToSubgroups = {
                                    backStack.add(Screen.SubgroupMgmt(screen.teamId))
                                },
                                onNavigateToEditTeam = { teamEditTeamId = screen.teamId },
                                onNavigateToInvite = { teamInviteTeamId = screen.teamId },
                                onNavigateToPlayerProfile = { userId ->
                                    backStack.add(Screen.PlayerProfile(screen.teamId, userId))
                                },
                                onNavigateToTeamStats = {
                                    backStack.add(Screen.TeamStats(screen.teamId))
                                },
                            )

                            is Screen.ClubProfileEdit -> ClubEditScreen(
                                clubId = screen.clubId,
                                onSaved = { backStack.removeLastOrNull() },
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            is Screen.PlayerProfile -> PlayerProfileScreen(
                                teamId = screen.teamId,
                                userId = screen.userId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            is Screen.PlayerStats -> PlayerStatsScreen(
                                userId = screen.userId,
                                teamId = screen.teamId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            is Screen.TeamStats -> TeamStatsScreen(
                                teamId = screen.teamId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToPlayerStats = { userId ->
                                    backStack.add(Screen.PlayerStats(userId = userId, teamId = screen.teamId))
                                },
                            )

                            is Screen.SubgroupMgmt -> SubgroupMgmtScreen(
                                teamId = screen.teamId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            is Screen.EventList -> EventListScreen(
                                teamId = screen.teamId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToDetail = { eventId -> backStack.add(Screen.EventDetail(eventId)) },
                                onNavigateToCreate = { teamId ->
                                    val dash = backStack.filterIsInstance<Screen.ClubDashboard>().lastOrNull()
                                    if (dash != null) backStack.add(Screen.EventForm(clubId = dash.clubId, preselectedTeamId = teamId))
                                },
                                onNavigateToCalendar = { teamId -> backStack.add(Screen.EventCalendar(teamId)) },
                            )

                            is Screen.EventCalendar -> EventCalendarScreen(
                                teamId = screen.teamId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToDetail = { eventId -> backStack.add(Screen.EventDetail(eventId)) },
                                onNavigateToList = { backStack.removeLastOrNull() },
                            )

                            is Screen.EventDetail -> EventDetailScreen(
                                eventId = screen.eventId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToEdit = { eventId, scope ->
                                    val dash = backStack.filterIsInstance<Screen.ClubDashboard>().lastOrNull()
                                    if (dash != null) backStack.add(Screen.EventForm(clubId = dash.clubId, eventId = eventId, editScope = scope.name))
                                },
                                onNavigateToDuplicate = { eventId ->
                                    val dash = backStack.filterIsInstance<Screen.ClubDashboard>().lastOrNull()
                                    if (dash != null) backStack.add(Screen.EventForm(clubId = dash.clubId, eventId = eventId))
                                },
                            )

                            is Screen.EventForm -> EventFormScreen(
                                clubId = screen.clubId,
                                eventId = screen.eventId,
                                preselectedTeamId = screen.preselectedTeamId,
                                editScope = RecurringScope.valueOf(screen.editScope),
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            is Screen.AttendanceList -> AttendanceListScreen(
                                eventId = screen.eventId,
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            Screen.MyAbsences -> MyAbsencesScreen(
                                onNavigateBack = { backStack.removeLastOrNull() },
                            )

                            // Phase 5+ screens — placeholder until migrated
                            else -> Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                },
            )
        }

        val clubId = coachInviteClubId
        if (clubId != null) {
            ClubCoachInviteSheet(
                clubId = clubId,
                onDismiss = { coachInviteClubId = null },
            )
        }

        val editTeamId = teamEditTeamId
        if (editTeamId != null) {
            TeamEditSheet(
                teamId = editTeamId,
                onSaved = { teamEditTeamId = null },
                onDismiss = { teamEditTeamId = null },
            )
        }

        val inviteTeamId = teamInviteTeamId
        if (inviteTeamId != null) {
            TeamInviteSheet(
                teamId = inviteTeamId,
                onDismiss = { teamInviteTeamId = null },
            )
        }
    }
}
