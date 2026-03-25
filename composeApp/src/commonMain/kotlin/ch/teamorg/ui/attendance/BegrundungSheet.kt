package ch.teamorg.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetBg = Color(0xFF13131F)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val DividerColor = Color(0xFF2A2A40)
private val BorderFocused = Color(0xFF4F8EF7)
private val ConfirmMaybeBg = Color(0xFF3D3400)
private val ConfirmMaybeText = Color(0xFFFACC15)
private val ConfirmDeclineBg = Color(0xFF450A0A)
private val ConfirmDeclineText = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BegrundungSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
    mode: String = "unsure"  // "unsure" | "declined"
) {
    if (!visible) return

    val isDecline = mode == "declined"
    val title = if (isDecline) "Why can't you go?" else "Why are you unsure?"
    val placeholder = if (isDecline) "Reason for declining (optional)..." else "Describe your reason..."
    val buttonLabel = if (isDecline) "Confirm Decline" else "Confirm Maybe"
    val buttonBg = if (isDecline) ConfirmDeclineBg else ConfirmMaybeBg
    val buttonText = if (isDecline) ConfirmDeclineText else ConfirmMaybeText
    val reasonRequired = !isDecline  // unsure requires reason, decline is optional

    var text by remember { mutableStateOf("") }

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
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider(color = DividerColor)

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(placeholder, color = TextMuted, fontSize = 14.sp)
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerColor,
                    focusedBorderColor = BorderFocused,
                    unfocusedTextColor = TextPrimary,
                    focusedTextColor = TextPrimary,
                    cursorColor = BorderFocused,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )

            val isEnabled = !reasonRequired || text.isNotBlank()
            Button(
                onClick = { if (isEnabled) onConfirm(text.trim()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = isEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonText,
                    disabledContainerColor = buttonBg.copy(alpha = 0.4f),
                    disabledContentColor = buttonText.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    buttonLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
