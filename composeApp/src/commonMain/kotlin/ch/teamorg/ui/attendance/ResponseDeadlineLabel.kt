package ch.teamorg.ui.attendance

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MutedForeground = Color(0xFF9090B0)
private val ColorGrey = Color(0xFF6B7280)

@Composable
fun ResponseDeadlineLabel(
    deadline: Instant?,
    modifier: Modifier = Modifier
) {
    if (deadline == null) return

    val now = Clock.System.now()
    val isPast = deadline <= now

    if (isPast) {
        Text(
            text = "Response closed",
            color = ColorGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
        )
    } else {
        val local = deadline.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val hour = local.hour.toString().padStart(2, '0')
        val min = local.minute.toString().padStart(2, '0')
        val formatted = "${local.dayOfMonth} $month at $hour:$min"
        Text(
            text = "Respond by $formatted",
            color = MutedForeground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
        )
    }
}
