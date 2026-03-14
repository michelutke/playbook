package com.playbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

@Composable
actual fun AppNavigation(
    backStack: MutableList<Screen>,
    isLoggedIn: Boolean
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeAt(backStack.lastIndex) },
        entryProvider = { screen ->
            NavEntry(screen) { ScreenContent(screen, backStack, isLoggedIn) }
        }
    )
}
