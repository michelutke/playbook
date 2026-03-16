package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.playbook.auth.AuthState
import com.playbook.auth.AuthViewModel
import com.playbook.navigation.AppNavigation
import com.playbook.navigation.Screen
import com.playbook.ui.components.PlaybookBottomBar
import com.playbook.ui.theme.PlaybookTheme
import com.playbook.di.kmpViewModel

@Composable
fun PlaybookApp(
    viewModel: AuthViewModel = kmpViewModel()
) {
    val authState by viewModel.state.collectAsState()
    val backStack = remember { mutableStateListOf<Screen>(Screen.Loading) }

    PlaybookTheme {
        LaunchedEffect(authState) {
            when (val state = authState) {
                is AuthState.Loading -> Unit
                is AuthState.Unauthenticated -> {
                    backStack.clear()
                    backStack.add(Screen.Login)
                }
                is AuthState.Authenticated -> {
                    backStack.clear()
                    backStack.add(if (!state.hasTeam) Screen.EmptyState else Screen.Events)
                }
            }
        }

        val currentScreen = backStack.lastOrNull() ?: Screen.Loading
        val showBottomBar = currentScreen !in listOf(
            Screen.Login, Screen.Register, Screen.EmptyState, Screen.Loading
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    PlaybookBottomBar(
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
