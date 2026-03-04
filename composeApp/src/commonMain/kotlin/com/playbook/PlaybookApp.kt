package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.playbook.auth.AuthState
import com.playbook.auth.AuthViewModel
import com.playbook.ui.clubsetup.ClubSetupScreen
import com.playbook.ui.components.PlaybookBottomBar
import com.playbook.ui.inviteaccept.InviteAcceptScreen
import com.playbook.ui.login.LoginScreen
import com.playbook.ui.teamsetup.TeamSetupScreen
import kotlinx.coroutines.flow.filter
import org.koin.compose.koinInject

@Composable
fun PlaybookApp(deepLinkToken: String? = null) {
    val authViewModel: AuthViewModel = koinInject()
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val currentScreen = backStack.lastOrNull()

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
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (val screen = currentScreen) {
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

                    // Phase 2+ screens — placeholder until migrated
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
