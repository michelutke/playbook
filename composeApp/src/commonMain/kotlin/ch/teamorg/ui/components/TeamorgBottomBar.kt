package ch.teamorg.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ch.teamorg.navigation.Screen

@Composable
fun TeamorgBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    unreadCount: Long = 0
) {
    // Bottom nav: 62dp height, cornerRadius 36dp
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(62.dp)
            .clip(MaterialTheme.shapes.extraLarge),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        BottomNavItem(
            screen = Screen.Events,
            icon = Icons.Default.Event,
            selected = currentRoute == Screen.Events.route || currentRoute == Screen.Calendar.route,
            onNavigate = onNavigate
        )
        BottomNavItem(
            screen = Screen.Teams,
            icon = Icons.Default.Groups,
            selected = currentRoute == Screen.Teams.route,
            onNavigate = onNavigate
        )
        // Inbox tab with unread badge
        NavigationBarItem(
            selected = currentRoute == Screen.Inbox.route,
            onClick = { onNavigate(Screen.Inbox) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge {
                                Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = Screen.Inbox.route)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )
        BottomNavItem(
            screen = Screen.Profile,
            icon = Icons.Default.Person,
            selected = currentRoute == Screen.Profile.route,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    screen: Screen,
    icon: ImageVector,
    selected: Boolean,
    onNavigate: (Screen) -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(screen) },
        icon = { Icon(icon, contentDescription = screen.route) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.surface
        )
    )
}
