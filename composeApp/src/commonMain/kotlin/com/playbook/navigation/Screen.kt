package com.playbook.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {
    @Serializable
    data object Events : Screen("events")
    
    @Serializable
    data object Calendar : Screen("calendar")
    
    @Serializable
    data object Teams : Screen("teams")
    
    @Serializable
    data object Inbox : Screen("inbox")
    
    @Serializable
    data object Profile : Screen("profile")
    
    // Auth screens
    @Serializable
    data object Login : Screen("login")
    
    @Serializable
    data object Register : Screen("register")
    
    @Serializable
    data object EmptyState : Screen("empty_state")
}
