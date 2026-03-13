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
    val navWrapper = rememberNavWrapper<Screen>(startDestination = Screen.Loading)
    
    PlaybookTheme {
        // Navigation logic based on AuthState
        LaunchedEffect(authState) {
            val state = authState
            when (state) {
                is AuthState.Loading -> {
                    navWrapper.manager.navigate(Screen.Loading)
                }
                is AuthState.Unauthenticated -> {
                    navWrapper.manager.navigate(Screen.Login) {
                        popUpTo(Screen.Loading) { inclusive = true }
                    }
                }
                is AuthState.Authenticated -> {
                    if (!state.hasTeam) {
                        navWrapper.manager.navigate(Screen.EmptyState) {
                            popUpTo(Screen.Loading) { inclusive = true }
                        }
                    } else {
                        navWrapper.manager.navigate(Screen.Events) {
                            popUpTo(Screen.Loading) { inclusive = true }
                        }
                    }
                }
            }
        }

        val currentEntry = navWrapper.manager.currentBackStack.lastOrNull()
        if (currentEntry == null) {
            Box(modifier = Modifier.fillMaxSize())
            return@PlaybookTheme
        }
        
        val currentScreen = currentEntry.destination as Screen

        val showBottomBar = when (currentScreen) {
            Screen.Login, Screen.Register, Screen.EmptyState, Screen.Loading -> false
            else -> true
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    PlaybookBottomBar(
                        currentRoute = currentScreen.route,
                        onNavigate = { screen ->
                            navWrapper.manager.navigate(screen)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AppNavigation(
                    navWrapper = navWrapper,
                    isLoggedIn = authState is AuthState.Authenticated
                )
            }
        }
    }
}
