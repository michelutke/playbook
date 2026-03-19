package ch.teamorg.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val WEEKDAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringPatternSheet(
    initialPattern: RecurringPatternState,
    onDone: (RecurringPatternState) -> Unit,
    onDismiss: () -> Unit
) {
    var patternType by remember { mutableStateOf(initialPattern.patternType) }
    var weekdays by remember { mutableStateOf(initialPattern.weekdays) }
    var intervalDays by remember { mutableStateOf(initialPattern.intervalDays) }
    var hasEndDate by remember { mutableStateOf(initialPattern.hasEndDate) }
    var endDate by remember { mutableStateOf(initialPattern.endDate) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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
            Text(
                "Recurring pattern",
                style = MaterialTheme.typography.titleLarge
            )

            // Repeat type radio group
            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "daily" to "Daily",
                    "weekly" to "Weekly",
                    "custom" to "Custom interval"
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = patternType == value,
                            onClick = { patternType = value }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Weekly: weekday grid
            if (patternType == "weekly") {
                Text("Days", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEKDAY_LABELS.forEachIndexed { index, label ->
                        FilterChip(
                            selected = index in weekdays,
                            onClick = {
                                weekdays = if (index in weekdays) weekdays - index else weekdays + index
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Custom: interval field
            if (patternType == "custom") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = intervalDays.toString(),
                        onValueChange = { text ->
                            val v = text.toIntOrNull()
                            if (v != null && v > 0) intervalDays = v
                        },
                        label = { Text("Every") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                    Text("days", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // End date toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("End date", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = hasEndDate,
                    onCheckedChange = { hasEndDate = it }
                )
            }
            if (hasEndDate) {
                val dateLabel = if (endDate != null) {
                    "${endDate!!.dayOfMonth} ${endDate!!.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${endDate!!.year}"
                } else "Select date"
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dateLabel)
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
                    onClick = {
                        onDone(
                            RecurringPatternState(
                                patternType = patternType,
                                weekdays = weekdays,
                                intervalDays = intervalDays,
                                hasEndDate = hasEndDate,
                                endDate = endDate
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }
            }
        }
    }

    if (showEndDatePicker) {
        RecurringEndDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEndDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val initMillis = initialDate?.let { d ->
        d.toEpochDays() * 24L * 60 * 60 * 1000
    } ?: (today.toEpochDays() * 24L * 60 * 60 * 1000)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val epochDays = (millis / (24L * 60 * 60 * 1000)).toInt()
                    onDateSelected(LocalDate.fromEpochDays(epochDays))
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
