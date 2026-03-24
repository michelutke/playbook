package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val SelectedFill = Color(0xFF4F8EF7)
private val UnselectedBorder = Color(0xFF2A2A40)
private val UnselectedBg = Color(0xFF13131F)
private val TextMuted = Color(0xFF6B7280)

private val WEEKDAY_SHORT = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
private val WEEKDAY_FULL = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun WeekdaySelector(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WEEKDAY_SHORT.forEachIndexed { index, label ->
            val isSelected = index in selectedDays
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SelectedFill else UnselectedBg)
                    .then(
                        if (!isSelected) Modifier.border(
                            width = 1.dp,
                            color = UnselectedBorder,
                            shape = CircleShape
                        ) else Modifier
                    )
                    .clickable { onToggle(index) }
                    .semantics { contentDescription = WEEKDAY_FULL[index] },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
