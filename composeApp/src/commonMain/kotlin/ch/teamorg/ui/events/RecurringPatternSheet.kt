package ch.teamorg.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// V1 color tokens
private val BottomSheetBg = Color(0xFF13131F)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF9090B0)
private val AccentBlue = Color(0xFF4F8EF7)
private val DividerColor = Color(0xFF2A2A40)
private val SectionLabel = Color(0xFF6B7280)
private val ToggleBg = Color(0xFF374151)

private val WEEKDAY_LABELS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringPatternSheet(
    initialPattern: RecurringPatternState,
    onDone: (RecurringPatternState) -> Unit,
    onDismiss: () -> Unit
) {
    // Derive initial frequencyUnit and intervalCount from the saved pattern
    val initFreqUnit = when (initialPattern.patternType) {
        "daily" -> "day"
        "weekly" -> "week"
        else -> {
            val d = initialPattern.intervalDays
            when {
                d > 0 && d % 30 == 0 -> "month"
                d > 0 && d % 7 == 0 -> "week"
                else -> "day"
            }
        }
    }
    val initCount = when (initFreqUnit) {
        "month" -> (initialPattern.intervalDays / 30).coerceAtLeast(1)
        "week" -> if (initialPattern.patternType == "weekly") 1 else (initialPattern.intervalDays / 7).coerceAtLeast(1)
        else -> if (initialPattern.patternType == "daily") 1 else initialPattern.intervalDays.coerceAtLeast(1)
    }

    var patternType by remember { mutableStateOf(initialPattern.patternType) }
    var weekdays by remember { mutableStateOf(initialPattern.weekdays) }
    var intervalCount by remember { mutableStateOf(initCount) }
    var frequencyUnit by remember { mutableStateOf(initFreqUnit) }
    var hasEndDate by remember { mutableStateOf(initialPattern.hasEndDate) }
    var endDate by remember { mutableStateOf(initialPattern.endDate) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Derive summary text from current UI state
    val summaryText = remember(patternType, weekdays, frequencyUnit, intervalCount) {
        when (frequencyUnit) {
            "day" -> if (intervalCount == 1) "Repeats daily" else "Repeats every $intervalCount days"
            "week" -> {
                if (weekdays.isEmpty()) {
                    if (intervalCount == 1) "Repeats weekly" else "Repeats every $intervalCount weeks"
                } else {
                    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    val days = weekdays.sorted().joinToString(" and ") { dayNames[it] }
                    "Repeats every $days"
                }
            }
            "month" -> if (intervalCount == 1) "Repeats monthly" else "Repeats every $intervalCount months"
            else -> "Repeats every $intervalCount ${frequencyUnit}s"
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Repeat Event",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = TextPrimary, fontSize = 14.sp)
                }
            }

            // Frequency row: "Every" + number input + Week/Month/Day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Every", color = TextPrimary, fontSize = 15.sp)

                // Number input
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = intervalCount.toString(),
                        onValueChange = { text ->
                            val v = text.toIntOrNull()
                            if (v != null && v > 0) intervalCount = v
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(AccentBlue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                // Frequency unit chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Week" to "week", "Month" to "month", "Day" to "day").forEach { (label, value) ->
                        val selected = frequencyUnit == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AccentBlue else CardBg)
                                .clickable {
                                    frequencyUnit = value
                                    patternType = when (value) {
                                        "day" -> "daily"
                                        "week" -> "weekly"
                                        else -> "custom"
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // "ON THESE DAYS" + 7 day circles (only for weekly)
            if (frequencyUnit == "week") {
                Text(
                    "ON THESE DAYS",
                    color = SectionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WEEKDAY_LABELS.forEachIndexed { index, label ->
                        val selected = index in weekdays
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selected) AccentBlue else CardBg)
                                .clickable {
                                    weekdays = if (index in weekdays) weekdays - index else weekdays + index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // End date row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83D\uDCC5", fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (hasEndDate && endDate != null) {
                        val d = endDate!!
                        "${d.dayOfMonth} ${d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${d.year}"
                    } else "Add end date",
                    color = if (hasEndDate && endDate != null) TextPrimary else TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (hasEndDate) showEndDatePicker = true
                        }
                )
                Switch(
                    checked = hasEndDate,
                    onCheckedChange = {
                        hasEndDate = it
                        if (it && endDate == null) showEndDatePicker = true
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccentBlue,
                        uncheckedTrackColor = ToggleBg,
                        uncheckedThumbColor = TextMuted,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            // Summary
            Text(
                summaryText,
                color = AccentBlue,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Apply button
            Button(
                onClick = {
                    val computedInterval = when (frequencyUnit) {
                        "day" -> intervalCount
                        "week" -> intervalCount * 7
                        "month" -> intervalCount * 30
                        else -> intervalCount * 7
                    }
                    onDone(
                        RecurringPatternState(
                            patternType = patternType,
                            weekdays = weekdays,
                            intervalDays = computedInterval,
                            hasEndDate = hasEndDate,
                            endDate = endDate
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    "Apply",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
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
