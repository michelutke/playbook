package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val SelectedFill = Color(0xFFEF4444)
private val UnselectedBg = Color(0xFF13131F)
private val UnselectedBorder = Color(0xFF2A2A40)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)

private val BODY_PARTS = listOf(
    listOf("Head", "Shoulder", "Chest", "Back", "Arm"),
    listOf("Hip", "Thigh", "Knee", "Shin", "Foot")
)

@Composable
fun BodyPartGrid(
    selectedParts: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BODY_PARTS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { part ->
                    val isSelected = part in selectedParts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SelectedFill else UnselectedBg)
                            .then(
                                if (!isSelected) Modifier.border(
                                    width = 1.dp,
                                    color = UnselectedBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .clickable { onToggle(part) }
                            .semantics { contentDescription = part },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = part,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
