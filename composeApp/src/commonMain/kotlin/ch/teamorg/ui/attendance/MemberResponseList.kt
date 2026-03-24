package ch.teamorg.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.CheckInEntry

private val ColorConfirmed = Color(0xFF22C55E)
private val ColorMaybe = Color(0xFFFACC15)
private val ColorDeclined = Color(0xFFEF4444)
private val ColorNoResponse = Color(0xFF6B7280)
private val TextMuted = Color(0xFF9090B0)
private val TextPrimary = Color(0xFFF0F0FF)

@Composable
fun MemberResponseList(
    entries: List<CheckInEntry>,
    isCoach: Boolean,
    onOverrideTap: (CheckInEntry, String) -> Unit  // entry + status button tapped
) {
    if (entries.isEmpty()) {
        EmptyAttendanceState()
        return
    }

    val confirmed = entries.filter { it.response?.status == "confirmed" }
    val maybe = entries.filter { it.response?.status == "unsure" }
    val declined = entries.filter { entry ->
        val s = entry.response?.status
        s == "declined" || s == "declined-auto"
    }
    val noResponse = entries.filter { entry ->
        val s = entry.response?.status
        s == null || s == "no-response"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (confirmed.isNotEmpty()) {
            ResponseSectionHeader(label = "CONFIRMED", count = confirmed.size, color = ColorConfirmed)
            confirmed.forEach { entry ->
                MemberResponseRow(
                    entry = entry,
                    isCoach = isCoach,
                    onStatusTap = { status -> onOverrideTap(entry, status) }
                )
            }
        }

        if (maybe.isNotEmpty()) {
            ResponseSectionHeader(label = "MAYBE", count = maybe.size, color = ColorMaybe)
            maybe.forEach { entry ->
                MemberResponseRow(
                    entry = entry,
                    isCoach = isCoach,
                    onStatusTap = { status -> onOverrideTap(entry, status) }
                )
            }
        }

        if (declined.isNotEmpty()) {
            ResponseSectionHeader(label = "DECLINED", count = declined.size, color = ColorDeclined)
            declined.forEach { entry ->
                MemberResponseRow(
                    entry = entry,
                    isCoach = isCoach,
                    onStatusTap = { status -> onOverrideTap(entry, status) }
                )
            }
        }

        if (noResponse.isNotEmpty()) {
            ResponseSectionHeader(label = "NO RESPONSE", count = noResponse.size, color = ColorNoResponse)
            noResponse.forEach { entry ->
                MemberResponseRow(
                    entry = entry,
                    isCoach = isCoach,
                    onStatusTap = { status -> onOverrideTap(entry, status) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResponseSectionHeader(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label  \u00b7  $count",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun EmptyAttendanceState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No responses yet",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Team members haven't responded to this event.",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
