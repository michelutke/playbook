package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetBg = Color(0xFF13131F)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val DividerColor = Color(0xFF2A2A40)
private val AccentBlue = Color(0xFF4F8EF7)

// Status button colors
private val PresentSelectedBg = Color(0xFF065F46)
private val PresentSelectedText = Color(0xFF22C55E)
private val AbsentSelectedBg = Color(0xFF450A0A)
private val AbsentSelectedText = Color(0xFFEF4444)
private val ExcusedSelectedBg = Color(0xFF3D3400)
private val ExcusedSelectedText = Color(0xFFFACC15)
private val InactiveBg = Color(0xFF1F2937)
private val InactiveText = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachOverrideSheet(
    visible: Boolean,
    playerName: String,
    currentStatus: String?,
    onDismiss: () -> Unit,
    onSave: (status: String, note: String?) -> Unit
) {
    if (!visible) return

    var selectedStatus by remember(currentStatus) { mutableStateOf(currentStatus) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DividerColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = "Close" }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Status label
            Text(
                text = "STATUS",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))

            // Status buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    label = "Present",
                    description = "Mark as Present",
                    selected = selectedStatus == "present",
                    selectedBg = PresentSelectedBg,
                    selectedText = PresentSelectedText,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "present" }
                )
                StatusButton(
                    label = "Absent",
                    description = "Mark as Absent",
                    selected = selectedStatus == "absent",
                    selectedBg = AbsentSelectedBg,
                    selectedText = AbsentSelectedText,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "absent" }
                )
                StatusButton(
                    label = "Excused",
                    description = "Mark as Excused",
                    selected = selectedStatus == "excused",
                    selectedBg = ExcusedSelectedBg,
                    selectedText = ExcusedSelectedText,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "excused" }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Note label
            Text(
                text = "NOTE (OPTIONAL)",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        text = "Add a note...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextPrimary)
            )

            Spacer(Modifier.height(16.dp))

            // Save CTA
            Button(
                onClick = { onSave(selectedStatus!!, note.ifBlank { null }) },
                enabled = selectedStatus != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = AccentBlue.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "Save Override",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    description: String,
    selected: Boolean,
    selectedBg: Color,
    selectedText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) selectedBg else InactiveBg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) selectedText else InactiveText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
