package ch.teamorg.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// V1 color tokens
private val BottomSheetBg = Color(0xFF13131F)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF9090B0)
private val AccentBlue = Color(0xFF4F8EF7)
private val DividerColor = Color(0xFF2A2A40)
private val SelectedBg = Color(0xFF172338)
private val SelectedIconBg = Color(0xFF1A3462)
private val UnselectedIconBg = Color(0xFF222234)
private val RadioBorder = Color(0xFF3A3A50)

/**
 * V1 EE-Modal for recurring events — edit/cancel scope selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScopeSheet(
    mode: String = "edit",
    onContinue: (scope: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf("this_only") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BottomSheetBg,
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                when (mode) {
                    "cancel" -> "Cancel which events?"
                    "uncancel" -> "Restore which events?"
                    else -> "Apply to which events?"
                },
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            // Subtitle
            Text(
                when (mode) {
                    "cancel" -> "This is a recurring event. Choose which events to cancel."
                    "uncancel" -> "This is a recurring event. Choose which events to restore."
                    else -> "This is a recurring event. Choose how to apply your changes."
                },
                color = TextMuted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(4.dp))

            // Option cards
            ScopeOptionCard(
                emoji = "\uD83D\uDCC5",
                label = "This event",
                selected = selectedScope == "this_only",
                onClick = { selectedScope = "this_only" }
            )
            ScopeOptionCard(
                emoji = "\u00BB",
                label = "This & following",
                selected = selectedScope == "this_and_future",
                onClick = { selectedScope = "this_and_future" }
            )
            ScopeOptionCard(
                emoji = "\uD83D\uDD01",
                label = "All events",
                selected = selectedScope == "all",
                onClick = { selectedScope = "all" }
            )

            Spacer(Modifier.height(4.dp))

            // Action button
            Button(
                onClick = { onContinue(selectedScope) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    when (mode) {
                        "cancel" -> "Cancel Events"
                        "uncancel" -> "Restore Events"
                        else -> "Update Events"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScopeOptionCard(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) SelectedBg else CardBg
    val iconBg = if (selected) SelectedIconBg else UnselectedIconBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }

        // Label
        Text(
            label,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Radio indicator
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(AccentBlue)
                    } else {
                        Modifier
                            .background(Color.Transparent)
                            .border(2.dp, RadioBorder, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
