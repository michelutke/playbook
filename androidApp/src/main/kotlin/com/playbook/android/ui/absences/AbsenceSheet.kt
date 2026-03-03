package com.playbook.android.ui.absences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.playbook.domain.AbwesenheitPresetType
import com.playbook.domain.AbwesenheitRule
import com.playbook.domain.AbwesenheitRuleType
import com.playbook.domain.CreateAbwesenheitRuleRequest
import com.playbook.domain.UpdateAbwesenheitRuleRequest
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceSheet(
    existingRule: AbwesenheitRule? = null,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AbsenceSheetViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saveState by viewModel.saveState.collectAsState()

    var selectedPreset by remember { mutableStateOf(existingRule?.presetType ?: AbwesenheitPresetType.HOLIDAYS) }
    var ruleType by remember { mutableStateOf(existingRule?.ruleType ?: AbwesenheitRuleType.RECURRING) }
    var selectedWeekdays by remember { mutableStateOf(existingRule?.weekdays?.toSet() ?: emptySet<Int>()) }
    var startDate by remember { mutableStateOf(existingRule?.startDate?.toString() ?: "") }
    var endDate by remember { mutableStateOf(existingRule?.endDate?.toString() ?: "") }
    var customLabel by remember { mutableStateOf(existingRule?.label ?: selectedPreset.defaultLabel) }
    var saveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveState) {
        when (saveState) {
            SaveState.Success -> {
                viewModel.resetSaveState()
                onSaved()
            }
            SaveState.Error -> {
                saveError = "Failed to save. Please try again."
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (existingRule != null) "Edit Absence" else "Add Absence",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))

            // Preset icon grid (2 rows × 3 cols)
            val presets = AbwesenheitPresetType.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { preset ->
                            val isSelected = preset == selectedPreset
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.transparent,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        selectedPreset = preset
                                        if (customLabel == selectedPreset.defaultLabel || customLabel.isBlank()) {
                                            customLabel = preset.defaultLabel
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(preset.icon, style = MaterialTheme.typography.titleLarge)
                                    Text(preset.defaultLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Rule type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = ruleType == AbwesenheitRuleType.RECURRING,
                    onClick = { ruleType = AbwesenheitRuleType.RECURRING },
                    label = { Text("Recurring") },
                )
                FilterChip(
                    selected = ruleType == AbwesenheitRuleType.PERIOD,
                    onClick = { ruleType = AbwesenheitRuleType.PERIOD },
                    label = { Text("Period") },
                )
            }

            Spacer(Modifier.height(12.dp))

            if (ruleType == AbwesenheitRuleType.RECURRING) {
                // Weekday multi-select M T W T F S S
                val weekdays = listOf(0 to "M", 1 to "T", 2 to "W", 3 to "T", 4 to "F", 5 to "S", 6 to "S")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekdays.forEach { (index, label) ->
                        val isSelected = index in selectedWeekdays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedWeekdays = if (isSelected) selectedWeekdays - index else selectedWeekdays + index
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("From (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("To (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (saveError != null) {
                Spacer(Modifier.height(4.dp))
                Text(saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    saveError = null
                    val parsedStart = startDate.toLocalDateOrNull()
                    val parsedEnd = endDate.toLocalDateOrNull()
                    val weekdayList = if (ruleType == AbwesenheitRuleType.RECURRING) selectedWeekdays.toList() else null
                    val label = customLabel.ifBlank { selectedPreset.defaultLabel }
                    if (existingRule != null) {
                        viewModel.updateRule(
                            existingRule.id,
                            UpdateAbwesenheitRuleRequest(
                                presetType = selectedPreset,
                                label = label,
                                ruleType = ruleType,
                                weekdays = weekdayList,
                                startDate = parsedStart,
                                endDate = parsedEnd,
                            ),
                        )
                    } else {
                        viewModel.createRule(
                            CreateAbwesenheitRuleRequest(
                                presetType = selectedPreset,
                                label = label,
                                ruleType = ruleType,
                                weekdays = weekdayList,
                                startDate = parsedStart,
                                endDate = parsedEnd,
                            ),
                        )
                    }
                },
                enabled = saveState != SaveState.Saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saveState == SaveState.Saving) "Saving…" else "Save")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    if (isBlank()) null else LocalDate.parse(this)
} catch (e: Exception) {
    null
}

private val androidx.compose.ui.graphics.Color.Companion.transparent: androidx.compose.ui.graphics.Color
    get() = androidx.compose.ui.graphics.Color.Transparent
