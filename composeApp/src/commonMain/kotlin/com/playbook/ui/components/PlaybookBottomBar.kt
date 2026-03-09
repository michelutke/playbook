package com.playbook.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

@Composable
fun PlaybookBottomBar(
    isHomeSelected: Boolean,
    isNotificationsSelected: Boolean,
    unreadCount: Int,
    onHomeClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = isHomeSelected,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            modifier = Modifier.semantics { testTag = "home_tab" },
        )
        NavigationBarItem(
            selected = isNotificationsSelected,
            onClick = onNotificationsClick,
            modifier = Modifier.semantics { testTag = "notifications_tab" },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            },
            label = { Text("Notifications") },
        )
    }
}
