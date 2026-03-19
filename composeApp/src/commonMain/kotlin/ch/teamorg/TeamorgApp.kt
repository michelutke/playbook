package ch.teamorg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ch.teamorg.auth.AuthState
import ch.teamorg.auth.AuthViewModel
import ch.teamorg.navigation.AppNavigation
import ch.teamorg.navigation.Screen
import ch.teamorg.ui.components.TeamorgBottomBar
import ch.teamorg.ui.theme.TeamorgTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.mp.KoinPlatform

@Composable
fun TeamorgApp(
    deepLinkToken: String? = null,
    viewModel: AuthViewModel = viewModel { KoinPlatform.getKoin().get() }
) {
    val authState by viewModel.state.collectAsState()
    val backStack = remember { mutableStateListOf<Screen>(Screen.Loading) }
    val pendingToken by DeepLinkHandler.pendingToken

    // Seed from launch-time deep link
    LaunchedEffect(Unit) {
        if (deepLinkToken != null) DeepLinkHandler.pendingToken.value = deepLinkToken
    }

    // Navigate to invite screen once auth state is known
    LaunchedEffect(authState, pendingToken) {
        val token = pendingToken
        if (token != null && authState !is AuthState.Loading) {
            DeepLinkHandler.pendingToken.value = null
            backStack.add(Screen.Invite(token))
        }
    }

    TeamorgTheme {
        LaunchedEffect(authState) {
            when (val state = authState) {
                is AuthState.Loading -> Unit
                is AuthState.Unauthenticated -> {
                    backStack.clear()
                    backStack.add(Screen.Login)
                }
                is AuthState.Authenticated -> {
                    if (backStack.none { it is Screen.Invite }) {
                        backStack.clear()
                        backStack.add(if (!state.hasTeam) Screen.EmptyState else Screen.Events)
                    }
                }
            }
        }

        val currentScreen = backStack.lastOrNull() ?: Screen.Loading
        val showBottomBar = currentScreen !in listOf(
            Screen.Login, Screen.Register, Screen.EmptyState, Screen.Loading
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = Color(0xFF090912),
            bottomBar = {
                if (showBottomBar) {
                    TeamorgBottomBar(
                        currentRoute = currentScreen.route,
                        onNavigate = { screen ->
                            backStack.add(screen)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AppNavigation(
                    backStack = backStack,
                    isLoggedIn = authState is AuthState.Authenticated
                )
            }
        }
    }
}
