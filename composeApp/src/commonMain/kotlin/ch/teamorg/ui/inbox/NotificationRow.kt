package ch.teamorg.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ch.teamorg.domain.Notification

private val CardBg = Color(0xFF1C1C2E)
private val PrimaryBlue = Color(0xFF4F8EF7)
private val MutedForeground = Color(0xFF9090B0)

@Composable
fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit
) {
    val iconTint = if (!notification.isRead) PrimaryBlue else MutedForeground
    val typeIcon = notificationIcon(notification.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = typeIcon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.labelSmall,
                color = MutedForeground
            )
            if (notification.body.isNotBlank()) {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF0F0FF),
                    maxLines = 2
                )
            }
            Text(
                text = formatRelativeTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MutedForeground.copy(alpha = 0.7f)
            )
        }
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
            )
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

private fun notificationIcon(type: String): ImageVector = when (type) {
    "event_new" -> Icons.Outlined.Event
    "event_edit" -> Icons.Outlined.EditNote
    "event_cancel" -> Icons.Outlined.EventBusy
    "reminder" -> Icons.Outlined.Alarm
    "response" -> Icons.Outlined.HowToVote
    "absence" -> Icons.Outlined.EventBusy
    else -> Icons.Outlined.Notifications
}

fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val epochMillis = parseIsoToMillis(isoTimestamp)
        val nowMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val diffMs = nowMillis - epochMillis
        val diffMin = diffMs / 60_000
        val diffHours = diffMin / 60
        val diffDays = diffHours / 24
        when {
            diffMin < 1 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays == 1L -> "1d ago"
            else -> "${diffDays}d ago"
        }
    } catch (_: Exception) {
        ""
    }
}

private fun parseIsoToMillis(iso: String): Long {
    // Parse ISO-8601 like "2026-03-26T08:18:14Z" or "2026-03-26T08:18:14.000Z"
    val cleaned = iso.trimEnd('Z').substringBefore('+').substringBefore('.').let {
        if (it.length == 19) it else iso.take(19)
    }
    // "2026-03-26T08:18:14"
    val parts = cleaned.split("T")
    val dateParts = parts[0].split("-").map { it.toInt() }
    val timeParts = parts[1].split(":").map { it.toInt() }

    val year = dateParts[0]
    val month = dateParts[1]
    val day = dateParts[2]
    val hour = timeParts[0]
    val minute = timeParts[1]
    val second = timeParts[2]

    // Days since Unix epoch (1970-01-01) via Zeller-style calculation
    var y = year.toLong()
    var m = month.toLong()
    if (m <= 2) { y--; m += 12 }
    val a = y / 100
    val b = 2 - a + a / 4
    val jd = (365.25 * (y + 4716)).toLong() + (30.6001 * (m + 1)).toLong() + day + b - 1524
    val epochDays = jd - 2440588L
    return epochDays * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1_000L
}
