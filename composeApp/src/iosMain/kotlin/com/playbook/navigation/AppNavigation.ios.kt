package com.playbook.navigation

import androidx.compose.runtime.Composable

/**
 * iOS navigation: renders the top screen directly.
 * NavDisplay from navigation3-ui depends on navigationevent-compose which
 * has no iOS/native variant — so we render the current screen without it.
 */
@Composable
actual fun AppNavigation(
    backStack: MutableList<Screen>,
    isLoggedIn: Boolean
) {
    val currentScreen = backStack.lastOrNull() ?: Screen.Loading
    ScreenContent(currentScreen, backStack, isLoggedIn)
}
