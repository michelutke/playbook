package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.CheckInEntry

private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val ColorGrey = Color(0xFF6B7280)
private val CardBg = Color(0xFF1C1C2E)
private val InactiveBg = Color(0xFF1F2937)
private val InactiveText = Color(0xFF6B7280)

@Composable
fun MemberResponseRow(
    entry: CheckInEntry,
    isCoach: Boolean,
    onStatusTap: (String) -> Unit    // "present"|"absent"|"excused"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (40dp circle with initials)
        val initials = entry.userName
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + reason column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.userName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                // Auto-declined indicator
                if (entry.response?.abwesenheitRuleId != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-declined",
                        color = ColorGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                // Coach override indicator
                val entryRecord = entry.record
                if (entryRecord != null && entryRecord.setBy != entry.userId) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✎",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.semantics { contentDescription = "Coach override" }
                    )
                }
            }
            val reason = entry.response?.reason
            if (!reason.isNullOrBlank()) {
                Text(
                    text = reason,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
        }

        // Coach override buttons
        if (isCoach) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CoachStatusButton(
                    symbol = "✓",
                    color = Color(0xFF22C55E),
                    contentDesc = "Set present",
                    onClick = { onStatusTap("present") }
                )
                CoachStatusButton(
                    symbol = "✗",
                    color = Color(0xFFEF4444),
                    contentDesc = "Set absent",
                    onClick = { onStatusTap("absent") }
                )
                CoachStatusButton(
                    symbol = "~",
                    color = Color(0xFFFACC15),
                    contentDesc = "Set excused",
                    onClick = { onStatusTap("excused") }
                )
            }
        }
    }
}

@Composable
private fun CoachStatusButton(
    symbol: String,
    color: Color,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(InactiveBg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
