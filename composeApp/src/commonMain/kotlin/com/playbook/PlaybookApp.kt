package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.NavBackStackEntry
import androidx.navigation3.rememberNavWrapper
import com.playbook.auth.AuthState
import com.playbook.auth.AuthViewModel
import com.playbook.navigation.AppNavigation
import com.playbook.navigation.Screen
import com.playbook.ui.components.PlaybookBottomBar
import com.playbook.ui.theme.PlaybookTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaybookApp(
    viewModel: AuthViewModel = koinViewModel()
) {
    val authState by viewModel.state.collectAsState()
    
    PlaybookTheme {
        val backstack = remember { mutableStateListOf<NavBackStackEntry<Screen>>() }
        
        // Navigation logic based on AuthState
        LaunchedEffect(authState) {
            val state = authState
            when (state) {
                is AuthState.Loading -> {
                    // Stay on splash/current until loaded
                }
                is AuthState.Unauthenticated -> {
                    backstack.clear()
                    backstack.add(NavBackStackEntry(Screen.Login, null))
                }
                is AuthState.Authenticated -> {
                    if (!state.hasTeam) {
                        backstack.clear()
                        backstack.add(NavBackStackEntry(Screen.EmptyState, null))
                    } else if (backstack.lastOrNull()?.destination !in listOf(Screen.Events, Screen.Calendar, Screen.Teams, Screen.Inbox, Screen.Profile)) {
                        backstack.clear()
                        backstack.add(NavBackStackEntry(Screen.Events, null))
                    }
                }
            }
        }

        if (backstack.isEmpty()) {
            // Loading screen or Splash
            Box(modifier = Modifier.fillMaxSize())
            return@PlaybookTheme
        }

        val currentEntry = backstack.last()
        val currentScreen = currentEntry.destination

        val showBottomBar = when (currentScreen) {
            Screen.Login, Screen.Register, Screen.EmptyState -> false
            else -> true
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    PlaybookBottomBar(
                        currentRoute = currentScreen.route,
                        onNavigate = { screen ->
                            if (backstack.last().destination != screen) {
                                backstack.clear()
                                backstack.add(NavBackStackEntry(screen, null))
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AppNavigation(
                    currentBackStack = backstack,
                    onNavigate = { screen ->
                        backstack.add(NavBackStackEntry(screen, null))
                    },
                    onBack = {
                        if (backstack.size > 1) {
                            backstack.removeAt(backstack.size - 1)
                        }
                    },
                    isLoggedIn = authState is AuthState.Authenticated
                )
            }
        }
    }
}
