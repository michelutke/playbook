package ch.teamorg.ui.attendance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.AbwesenheitRule

private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val AccentBlue = Color(0xFF4F8EF7)
private val SuccessGreen = Color(0xFF22C55E)
private val InactiveGrey = Color(0xFF6B7280)

private fun iconForPresetType(presetType: String): ImageVector = when (presetType.lowercase()) {
    "holidays" -> Icons.Outlined.WbSunny
    "injury" -> Icons.Outlined.FlashOn
    "work" -> Icons.Outlined.Work
    "school" -> Icons.Outlined.MenuBook
    "travel" -> Icons.Outlined.Flight
    else -> Icons.Outlined.MoreHoriz
}

private fun formatDateRange(rule: AbwesenheitRule): String {
    return when (rule.ruleType) {
        "period" -> {
            val from = rule.startDate ?: ""
            val to = rule.endDate ?: ""
            if (from.isNotEmpty() && to.isNotEmpty()) "$from – $to"
            else if (from.isNotEmpty()) "From $from"
            else "No date set"
        }
        "recurring" -> {
            val days = rule.weekdays
            if (!days.isNullOrEmpty()) {
                val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                "Every " + days.sorted().joinToString(", ") { names[it] }
            } else "Recurring"
        }
        else -> rule.startDate ?: ""
    }
}

private fun isActive(rule: AbwesenheitRule): Boolean {
    val endDate = rule.endDate ?: return true
    // Simple string comparison works for ISO dates
    val today = kotlinx.datetime.Clock.System.now()
        .toString().take(10) // "YYYY-MM-DD"
    return endDate >= today
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AbsenceCard(
    rule: AbwesenheitRule,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = iconForPresetType(rule.presetType),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AccentBlue
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.label,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = formatDateRange(rule),
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        // Status badge
        val active = isActive(rule)
        Surface(
            color = if (active) SuccessGreen.copy(alpha = 0.15f) else InactiveGrey.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (active) "Active" else "Ended",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (active) SuccessGreen else InactiveGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
