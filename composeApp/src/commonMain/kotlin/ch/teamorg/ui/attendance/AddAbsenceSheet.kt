package ch.teamorg.ui.attendance

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.AbwesenheitRule
import ch.teamorg.domain.CreateAbwesenheitRequest
import kotlinx.datetime.LocalDate

private val SheetBg = Color(0xFF13131F)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val DividerColor = Color(0xFF2A2A40)
private val AccentOrange = Color(0xFFF97316)
private val AccentBlue = Color(0xFF4F8EF7)

private data class ReasonPreset(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private val REASON_PRESETS = listOf(
    ReasonPreset("holidays", "Holidays", Icons.Outlined.WbSunny),
    ReasonPreset("injury", "Injury", Icons.Outlined.FlashOn),
    ReasonPreset("work", "Work", Icons.Outlined.Work),
    ReasonPreset("school", "School", Icons.Outlined.MenuBook),
    ReasonPreset("travel", "Travel", Icons.Outlined.Flight),
    ReasonPreset("other", "Other", Icons.Outlined.MoreHoriz)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAbsenceSheet(
    visible: Boolean,
    editingRule: AbwesenheitRule? = null,
    onDismiss: () -> Unit,
    onSave: (CreateAbwesenheitRequest) -> Unit
) {
    if (!visible) return

    // Pre-fill from editingRule if provided
    var selectedReason by remember(editingRule) {
        mutableStateOf(editingRule?.presetType ?: "")
    }
    var selectedBodyParts by remember(editingRule) {
        mutableStateOf(
            editingRule?.bodyPart?.let { setOf(it) } ?: emptySet<String>()
        )
    }
    var ruleType by remember(editingRule) {
        mutableStateOf(editingRule?.ruleType ?: "recurring")
    }
    var selectedWeekdays by remember(editingRule) {
        mutableStateOf(editingRule?.weekdays?.toSet() ?: emptySet<Int>())
    }
    var startDate by remember(editingRule) { mutableStateOf(editingRule?.startDate ?: "") }
    var endDate by remember(editingRule) { mutableStateOf(editingRule?.endDate ?: "") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val title = if (editingRule != null) "Edit Absence" else "Add Absence"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxHeight(),
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
                .fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
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

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reason section
                SectionLabel("REASON")
                Spacer(Modifier.height(4.dp))

                // 2x3 grid of reason tiles
                val rows = REASON_PRESETS.chunked(3)
                rows.forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            AbsenceReasonTile(
                                label = preset.label,
                                icon = preset.icon,
                                selected = selectedReason == preset.key,
                                onClick = { selectedReason = preset.key },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Body part grid — only for Injury
                Column(modifier = Modifier.animateContentSize()) {
                    if (selectedReason == "injury") {
                        Spacer(Modifier.height(4.dp))
                        SectionLabel("AFFECTED AREA")
                        Spacer(Modifier.height(8.dp))
                        BodyPartGrid(
                            selectedParts = selectedBodyParts,
                            onToggle = { part ->
                                selectedBodyParts = if (part in selectedBodyParts) {
                                    selectedBodyParts - part
                                } else {
                                    selectedBodyParts + part
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Type section
                SectionLabel("TYPE")
                Spacer(Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { ruleType = "recurring" },
                        selected = ruleType == "recurring",
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = AccentBlue.copy(alpha = 0.15f),
                            activeContentColor = AccentBlue,
                            inactiveContainerColor = CardBg,
                            inactiveContentColor = TextMuted
                        )
                    ) {
                        Text("Recurring")
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { ruleType = "period" },
                        selected = ruleType == "period",
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = AccentBlue.copy(alpha = 0.15f),
                            activeContentColor = AccentBlue,
                            inactiveContainerColor = CardBg,
                            inactiveContentColor = TextMuted
                        )
                    ) {
                        Text("Period")
                    }
                }

                if (ruleType == "recurring") {
                    SectionLabel("DAYS")
                    Spacer(Modifier.height(4.dp))
                    WeekdaySelector(
                        selectedDays = selectedWeekdays,
                        onToggle = { day ->
                            selectedWeekdays = if (day in selectedWeekdays) {
                                selectedWeekdays - day
                            } else {
                                selectedWeekdays + day
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("END DATE (OPTIONAL)")
                    Spacer(Modifier.height(4.dp))
                    DatePickerField(
                        value = endDate,
                        placeholder = "Select end date",
                        onClick = { showEndDatePicker = true }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePickerField(
                            value = startDate,
                            placeholder = "From",
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            value = endDate,
                            placeholder = "To",
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val label = REASON_PRESETS
                            .firstOrNull { it.key == selectedReason }?.label
                            ?: selectedReason
                        val bodyPart = selectedBodyParts.firstOrNull()
                        onSave(
                            CreateAbwesenheitRequest(
                                presetType = selectedReason,
                                label = label,
                                bodyPart = bodyPart,
                                ruleType = ruleType,
                                weekdays = if (ruleType == "recurring") selectedWeekdays.sorted() else null,
                                startDate = if (ruleType == "period") startDate.ifBlank { null } else null,
                                endDate = endDate.ifBlank { null }
                            )
                        )
                    },
                    enabled = selectedReason.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        disabledContainerColor = AccentOrange.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "Save Rule",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        AbsenceDatePickerDialog(
            onDateSelected = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        AbsenceDatePickerDialog(
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun DatePickerField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = DividerColor,
                disabledTextColor = TextPrimary,
                disabledPlaceholderColor = TextMuted,
                disabledContainerColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenceDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val epochDays = (millis / (24L * 60 * 60 * 1000)).toInt()
                    val date = LocalDate.fromEpochDays(epochDays)
                    onDateSelected("${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}")
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
