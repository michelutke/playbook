package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// RSVP state colors (from UI-SPEC)
private val GoingSelectedBg = Color(0xFF065F46)
private val GoingSelectedText = Color(0xFF22C55E)
private val MaybeSelectedBg = Color(0xFF3D3400)
private val MaybeSelectedText = Color(0xFFFACC15)
private val DeclinedSelectedBg = Color(0xFF450A0A)
private val DeclinedSelectedText = Color(0xFFEF4444)
private val InactiveBg = Color(0xFF1F2937)
private val InactiveText = Color(0xFF6B7280)
private val DisabledBg = Color(0xFF13131F)

@Composable
fun AttendanceRsvpButtons(
    currentResponse: String?,          // "confirmed"|"declined"|"unsure"|null
    confirmedCount: Int,
    maybeCount: Int,
    declinedCount: Int,
    deadlinePassed: Boolean,
    compact: Boolean = false,          // true = 32dp for list cards, false = 48dp for detail
    onSelect: (String) -> Unit         // "confirmed"|"unsure"|"declined"
) {
    val height = if (compact) 32.dp else 48.dp
    val cornerRadius = if (compact) 6.dp else 8.dp
    val iconSize = if (compact) 14.sp else 16.sp
    val countSize = 12.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Going button
        val goingSelected = currentResponse == "confirmed"
        RsvpButton(
            modifier = Modifier.weight(1f),
            symbol = "✓",
            label = "Going",
            count = confirmedCount,
            isSelected = goingSelected,
            selectedBg = GoingSelectedBg,
            selectedText = GoingSelectedText,
            deadlinePassed = deadlinePassed,
            compact = compact,
            height = height.value.toInt(),
            cornerRadius = cornerRadius.value.toInt(),
            iconSize = iconSize,
            countSize = countSize,
            onClick = { if (!deadlinePassed) onSelect("confirmed") },
            contentDesc = "Going"
        )

        // Maybe button
        val maybeSelected = currentResponse == "unsure"
        RsvpButton(
            modifier = Modifier.weight(1f),
            symbol = "?",
            label = "Maybe",
            count = maybeCount,
            isSelected = maybeSelected,
            selectedBg = MaybeSelectedBg,
            selectedText = MaybeSelectedText,
            deadlinePassed = deadlinePassed,
            compact = compact,
            height = height.value.toInt(),
            cornerRadius = cornerRadius.value.toInt(),
            iconSize = iconSize,
            countSize = countSize,
            onClick = { if (!deadlinePassed) onSelect("unsure") },
            contentDesc = "Maybe"
        )

        // Can't Go button
        val declinedSelected = currentResponse == "declined"
        RsvpButton(
            modifier = Modifier.weight(1f),
            symbol = "✗",
            label = "Can't Go",
            count = declinedCount,
            isSelected = declinedSelected,
            selectedBg = DeclinedSelectedBg,
            selectedText = DeclinedSelectedText,
            deadlinePassed = deadlinePassed,
            compact = compact,
            height = height.value.toInt(),
            cornerRadius = cornerRadius.value.toInt(),
            iconSize = iconSize,
            countSize = countSize,
            onClick = { if (!deadlinePassed) onSelect("declined") },
            contentDesc = "Can't Go"
        )
    }
}

@Composable
private fun RsvpButton(
    modifier: Modifier,
    symbol: String,
    label: String,
    count: Int,
    isSelected: Boolean,
    selectedBg: Color,
    selectedText: Color,
    deadlinePassed: Boolean,
    compact: Boolean,
    height: Int,
    cornerRadius: Int,
    iconSize: androidx.compose.ui.unit.TextUnit,
    countSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    contentDesc: String
) {
    val bg = when {
        deadlinePassed -> DisabledBg
        isSelected -> selectedBg
        else -> InactiveBg
    }
    val textColor = when {
        deadlinePassed -> InactiveText
        isSelected -> selectedText
        else -> InactiveText
    }
    val alpha = if (deadlinePassed) 0.5f else 1f

    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(bg)
            .alpha(alpha)
            .then(if (!deadlinePassed) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        if (compact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(symbol, color = textColor, fontSize = iconSize, fontWeight = FontWeight.SemiBold)
                Text(count.toString(), color = textColor, fontSize = countSize, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(symbol, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}
