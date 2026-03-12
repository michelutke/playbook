package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.NavBackStackEntry
import com.playbook.navigation.AppNavigation
import com.playbook.navigation.Screen
import com.playbook.ui.components.PlaybookBottomBar
import com.playbook.ui.theme.PlaybookTheme

@Composable
fun PlaybookApp() {
    PlaybookTheme {
        // Simple navigation state for now until AuthViewModel is ready in Plan 04
        val backstack = remember { mutableStateListOf(NavBackStackEntry(Screen.Events, null)) }
        val currentEntry = backstack.last()
        val currentScreen = currentEntry.destination

        // Bottom nav is hidden on auth screens
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
                            // Simple nav logic for placeholder
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
                    }
                )
            }
        }
    }
}
