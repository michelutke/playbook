package ch.teamorg.ui.attendance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val ProgressFill = Color(0xFF4F8EF7)
private val ProgressTrack = Color(0xFF1C1C2E)
private val TextMuted = Color(0xFF9090B0)

@Composable
fun AttendanceStatsBar(
    presencePct: Float,
    label: String = "Attendance",
    modifier: Modifier = Modifier
) {
    val animatedPct by animateFloatAsState(
        targetValue = presencePct.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "attendance_progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label \u00B7 ${(presencePct * 100).roundToInt()}%",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedPct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = ProgressFill,
            trackColor = ProgressTrack
        )
    }
}
