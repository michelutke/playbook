package com.playbook.ui.eventform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.playbook.domain.EventType
import com.playbook.domain.PatternType
import com.playbook.domain.RecurringScope
import com.playbook.ui.components.label
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EventFormScreen(
    clubId: String,
    eventId: String?,
    preselectedTeamId: String?,
    editScope: RecurringScope,
    onNavigateBack: () -> Unit,
    viewModel: EventFormViewModel = koinViewModel { parametersOf(clubId, eventId, preselectedTeamId, editScope) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                EventFormEvent.NavigateBack -> onNavigateBack()
            }
        }
    }
    EventFormContent(state = state, onAction = viewModel::submitAction, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFormContent(
    state: EventFormScreenState,
    onAction: (EventFormAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val isEdit = state.mode == EventFormMode.EDIT
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Event" else "Create Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { FormSection("Basic") }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onAction(EventFormAction.TitleChanged(it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(it) } },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EventType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { onAction(EventFormAction.TypeSelected(type)) },
                            shape = SegmentedButtonDefaults.itemShape(index, EventType.entries.size),
                            label = { Text(type.label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            item { FormSection("Time") }
            item {
                DateTimeRow(
                    label = "Start",
                    date = state.startDate,
                    time = state.startTime,
                    onDateChange = { onAction(EventFormAction.StartDateChanged(it)) },
                    onTimeChange = { onAction(EventFormAction.StartTimeChanged(it)) },
                )
            }
            item {
                DateTimeRow(
                    label = "End",
                    date = state.endDate,
                    time = state.endTime,
                    onDateChange = { onAction(EventFormAction.EndDateChanged(it)) },
                    onTimeChange = { onAction(EventFormAction.EndTimeChanged(it)) },
                )
                if (state.timeError != null) {
                    Text(state.timeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
            }

            item { FormSection("Location") }
            item {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { onAction(EventFormAction.LocationChanged(it)) },
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.availableTeams.isNotEmpty()) {
                item { FormSection("Audience") }
                item {
                    Text("Teams", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.availableTeams.forEach { team ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAction(EventFormAction.TeamToggled(team.id)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = team.id in state.selectedTeamIds,
                                onCheckedChange = { onAction(EventFormAction.TeamToggled(team.id)) },
                            )
                            Text(team.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                val visibleSubgroups = state.selectedTeamIds
                    .flatMap { teamId -> state.availableSubgroups[teamId] ?: emptyList() }
                if (visibleSubgroups.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Sub-groups (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Leave empty to target whole team", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        visibleSubgroups.forEach { subgroup ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onAction(EventFormAction.SubgroupToggled(subgroup.id)) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = subgroup.id in state.selectedSubgroupIds,
                                    onCheckedChange = { onAction(EventFormAction.SubgroupToggled(subgroup.id)) },
                                )
                                Text(subgroup.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item { FormSection("Options") }
            item {
                OutlinedTextField(
                    value = state.minAttendees,
                    onValueChange = { onAction(EventFormAction.MinAttendeesChanged(it)) },
                    label = { Text("Min attendees (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onAction(EventFormAction.DescriptionChanged(it)) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
                Spacer(Modifier.height(8.dp))
            }

            item { FormSection("Recurring") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Recurring event", style = MaterialTheme.typography.bodyMedium)
                        if (state.isRecurring) {
                            val summary = when (state.patternType) {
                                PatternType.DAILY -> "Daily"
                                PatternType.WEEKLY -> {
                                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    val days = state.selectedWeekdays.sorted().joinToString(", ") { dayNames[it - 1] }
                                    "Weekly${if (days.isNotBlank()) " · $days" else ""}"
                                }
                                PatternType.CUSTOM -> "Every ${state.intervalDays} days"
                            }
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Switch(
                        checked = state.isRecurring,
                        onCheckedChange = { onAction(EventFormAction.RecurringToggled(it)) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = { onAction(EventFormAction.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text(if (isEdit) "Save" else "Create")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (state.showPatternSheet) {
        RecurringPatternSheet(state = state, onAction = onAction)
    }
}

@Composable
private fun FormSection(title: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
) {
    val h = time.hour.toString().padStart(2, '0')
    val m = time.minute.toString().padStart(2, '0')
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        DateChip(
            text = "${date.dayOfMonth} ${date.month.name.take(3)} ${date.year}",
            onClick = { /* TODO: show date picker */ },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        DateChip(
            text = "$h:$m",
            onClick = { /* TODO: show time picker */ },
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
private fun DateChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringPatternSheet(state: EventFormScreenState, onAction: (EventFormAction) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onAction(EventFormAction.PatternSheetDismissed) },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recurring Pattern", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Repeat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            PatternType.entries.forEach { pattern ->
                val label = when (pattern) {
                    PatternType.DAILY -> "Daily"
                    PatternType.WEEKLY -> "Weekly"
                    PatternType.CUSTOM -> "Custom interval (days)"
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onAction(EventFormAction.PatternTypeSelected(pattern)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.patternType == pattern, onClick = { onAction(EventFormAction.PatternTypeSelected(pattern)) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (state.patternType == PatternType.WEEKLY) {
                Spacer(Modifier.height(12.dp))
                Text("Days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        val weekday = index + 1
                        val selected = weekday in state.selectedWeekdays
                        FilterChip(
                            selected = selected,
                            onClick = { onAction(EventFormAction.WeekdayToggled(weekday)) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            if (state.patternType == PatternType.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.intervalDays.toString(),
                    onValueChange = { onAction(EventFormAction.IntervalDaysChanged(it)) },
                    label = { Text("Interval (days)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onAction(EventFormAction.PatternSheetDismissed) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
