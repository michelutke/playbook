package ch.teamorg.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * S6 — Edit/Cancel recurring scope sheet.
 *
 * @param mode "edit" or "cancel" — controls header title
 * @param onContinue called with selected scope: "this_only" | "this_and_future" | "all"
 * @param onDismiss called when user taps Cancel or drags sheet down
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScopeSheet(
    mode: String = "edit",
    onContinue: (scope: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf("this_only") }

    val header = if (mode == "cancel") "Cancel recurring event" else "Edit recurring event"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(header, style = MaterialTheme.typography.titleLarge)

            // Radio options
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "this_only" to "This event only",
                    "this_and_future" to "This and future events",
                    "all" to "All events in series"
                ).forEach { (scope, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onContinue(selectedScope) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
