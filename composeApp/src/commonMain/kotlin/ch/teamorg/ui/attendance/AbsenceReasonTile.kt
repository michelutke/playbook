package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val SelectedBorder = Color(0xFF4F8EF7)
private val SelectedBg = Color(0xFF1C1C2E)
private val UnselectedBorder = Color(0xFF2A2A40)
private val UnselectedBg = Color(0xFF13131F)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)

@Composable
fun AbsenceReasonTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) SelectedBorder else UnselectedBorder
    val bgColor = if (selected) SelectedBg else UnselectedBg

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) SelectedBorder else TextMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = if (selected) TextPrimary else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
