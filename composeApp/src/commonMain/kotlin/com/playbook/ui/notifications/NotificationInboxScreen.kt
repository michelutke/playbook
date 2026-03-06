package com.playbook.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playbook.domain.DeepLinkDestination
import com.playbook.domain.Notification
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.playbook.di.kmpViewModel
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    onNavigate: (DeepLinkDestination) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NotificationInboxViewModel = kmpViewModel(),
) {
    val state by viewModel.state.collectAsState()
    viewModel.onNavigate = onNavigate

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    TextButton(onClick = { viewModel.submitAction(NotificationInboxAction.MarkAllRead) }) {
                        Text("Mark all read")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.notifications.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You're all caught up", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val grouped = groupByDate(state.notifications)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    grouped.forEach { (label, items) ->
                        item {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(items, key = { it.id }) { notification ->
                            SwipeToDeleteNotificationItem(
                                notification = notification,
                                onTap = { viewModel.submitAction(NotificationInboxAction.TapNotification(notification)) },
                                onDelete = { viewModel.submitAction(NotificationInboxAction.Delete(notification.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteNotificationItem(
    notification: Notification,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        NotificationRow(notification = notification, onTap = onTap)
    }
}

@Composable
private fun NotificationRow(notification: Notification, onTap: () -> Unit) {
    val bgModifier = if (!notification.read) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(bgModifier)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationIcon(notification.type),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                relativeTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!notification.read) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun notificationIcon(type: String): ImageVector = when (type) {
    "event_created" -> Icons.Outlined.NotificationsActive
    "event_updated" -> Icons.Outlined.Edit
    "event_cancelled" -> Icons.Outlined.Cancel
    "event_reminder" -> Icons.Outlined.Timer
    "attendance_response" -> Icons.Outlined.CheckCircle
    "attendance_summary" -> Icons.Outlined.Summarize
    "abwesenheit_change" -> Icons.Outlined.BeachAccess
    else -> Icons.Outlined.NotificationsActive
}

private fun groupByDate(notifications: List<Notification>): List<Pair<String, List<Notification>>> {
    val now = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(tz).date
    val yesterday = (now - 1.days).toLocalDateTime(tz).date

    val todayItems = mutableListOf<Notification>()
    val yesterdayItems = mutableListOf<Notification>()
    val earlierItems = mutableListOf<Notification>()

    notifications.forEach { n ->
        val date = Instant.parse(n.createdAt).toLocalDateTime(tz).date
        when (date) {
            today -> todayItems.add(n)
            yesterday -> yesterdayItems.add(n)
            else -> earlierItems.add(n)
        }
    }

    return buildList {
        if (todayItems.isNotEmpty()) add("Today" to todayItems)
        if (yesterdayItems.isNotEmpty()) add("Yesterday" to yesterdayItems)
        if (earlierItems.isNotEmpty()) add("Earlier" to earlierItems)
    }
}

private fun relativeTime(isoString: String): String {
    return try {
        val then = Instant.parse(isoString)
        val diff = Clock.System.now() - then
        when {
            diff < 1.minutes -> "Just now"
            diff < 1.hours -> "${diff.inWholeMinutes}m ago"
            diff < 1.days -> "${diff.inWholeHours}h ago"
            diff < 7.days -> "${diff.inWholeDays}d ago"
            else -> {
                val local = then.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.dayOfMonth} ${local.month.name.take(3)}"
            }
        }
    } catch (_: Exception) {
        ""
    }
}
