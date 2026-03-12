package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.rememberNavController
import androidx.navigation3.NavBackStackEntry
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
    val navController = rememberNavController<Screen>(startDestination = Screen.Loading)
    
    PlaybookTheme {
        // Navigation logic based on AuthState
        LaunchedEffect(authState) {
            val state = authState
            when (state) {
                is AuthState.Loading -> {
                    navController.navigate(Screen.Loading)
                }
                is AuthState.Unauthenticated -> {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Loading) { inclusive = true }
                    }
                }
                is AuthState.Authenticated -> {
                    if (!state.hasTeam) {
                        navController.navigate(Screen.EmptyState) {
                            popUpTo(Screen.Loading) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Events) {
                            popUpTo(Screen.Loading) { inclusive = true }
                        }
                    }
                }
            }
        }

        val currentEntry = navController.currentBackStack.lastOrNull()
        if (currentEntry == null) {
            Box(modifier = Modifier.fillMaxSize())
            return@PlaybookTheme
        }
        
        val currentScreen = currentEntry.destination

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
                            navController.navigate(screen)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AppNavigation(
                    navController = navController,
                    isLoggedIn = authState is AuthState.Authenticated
                )
            }
        }
    }
}