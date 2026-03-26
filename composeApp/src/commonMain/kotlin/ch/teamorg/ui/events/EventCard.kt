package ch.teamorg.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.ui.attendance.AttendanceRsvpButtons
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Card background colors per event type (design spec)
private val CardBgTraining = Color(0xFF1A2744)
private val CardBgMatch = Color(0xFF0F2218)
private val CardBgOther = Color(0xFF1C1040)
private val CardBgCancelled = Color(0xFF1A1A28)

// Text colors
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF9CA3AF)
private val TextTime = Color(0xFF6B7280)
private val TextCancelledMuted = Color(0xFF6B7280)

// Cancelled badge colors
private val CancelledBadgeText = Color(0xFFEF4444)
private val CancelledBadgeBg = Color(0xFF4B1A1A)

private fun cardBgColor(type: String, isCancelled: Boolean): Color = when {
    isCancelled -> CardBgCancelled
    type == "training" -> CardBgTraining
    type == "match" -> CardBgMatch
    else -> CardBgOther
}

private fun formatTwoDigits(n: Int): String = n.toString().padStart(2, '0')

private fun dayAbbrev(instant: kotlinx.datetime.Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return local.dayOfWeek.name.take(3).uppercase()
}

private fun dayMonth(instant: kotlinx.datetime.Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth}.${formatTwoDigits(local.monthNumber)}"
}

private fun formatHHmm(instant: kotlinx.datetime.Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${formatTwoDigits(local.hour)}:${formatTwoDigits(local.minute)}"
}

private fun buildTimeLine(ewt: EventWithTeams): String {
    val event = ewt.event
    val start = formatHHmm(event.startAt)
    val end = formatHHmm(event.endAt)
    val meetupAt = event.meetupAt
    return if (meetupAt != null) {
        val meet = formatHHmm(meetupAt)
        "Meet $meet · $start – $end"
    } else {
        "$start – $end"
    }
}

@Composable
fun EventCard(
    ewt: EventWithTeams,
    confirmedCount: Int,
    maybeCount: Int,
    declinedCount: Int,
    myResponse: String?,
    onClick: () -> Unit,
    onRsvpSelect: (String) -> Unit
) {
    val event = ewt.event
    val isCancelled = event.status == "cancelled"
    val bgColor = cardBgColor(event.type, isCancelled)
    val titleColor = if (isCancelled) TextCancelledMuted else TextPrimary
    val dateColor = if (isCancelled) TextCancelledMuted else TextPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top row: title+team on left, day abbrev+date on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = event.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val teamName = ewt.matchedTeams.firstOrNull()?.name
                    if (teamName != null) {
                        Text(
                            text = teamName,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = dayAbbrev(event.startAt),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                    Text(
                        text = dayMonth(event.startAt),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = dateColor
                    )
                }
            }

            // Times row
            Text(
                text = buildTimeLine(ewt),
                fontSize = 12.sp,
                color = TextTime
            )

            // RSVP row or Cancelled badge
            if (isCancelled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CancelledBadgeBg)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Cancelled",
                        fontSize = 12.sp,
                        color = CancelledBadgeText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                AttendanceRsvpButtons(
                    currentResponse = myResponse,
                    confirmedCount = confirmedCount,
                    maybeCount = maybeCount,
                    declinedCount = declinedCount,
                    deadlinePassed = false,
                    compact = true,
                    onSelect = onRsvpSelect
                )
            }
        }
    }
}
