package ch.teamorg.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val ColorTraining = Color(0xFF4F8EF7)
private val ColorMatch = Color(0xFF22C55E)
private val ColorOther = Color(0xFFA855F7)
private val ColorMuted = Color(0xFF9090B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEventScreen(
    viewModel: CreateEditEventViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showRecurringSheet by remember { mutableStateOf(false) }
    var showScopeSheet by remember { mutableStateOf(false) }

    // Date/time picker dialog states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showMeetupTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FormEvent.SaveSuccess -> onSaved()
                is FormEvent.CancelSuccess -> onSaved()
            }
        }
    }

    // Auto-open recurring sheet when recurring is enabled without a pattern
    LaunchedEffect(state.recurringEnabled, state.recurringPattern) {
        if (state.recurringEnabled && state.recurringPattern == null) {
            showRecurringSheet = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Event" else "New Event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            CtaBar(
                isEditMode = state.isEditMode,
                isSaving = state.isSaving,
                isSeriesEvent = state.isSeriesEvent,
                onSave = {
                    if (state.isEditMode && state.isSeriesEvent) {
                        showScopeSheet = true
                    } else {
                        viewModel.save()
                    }
                },
                onCancel = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Section 1: Basic
            SectionHeader("Basic")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title field
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text("Event title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.titleError != null,
                    singleLine = true
                )
                if (state.titleError != null) {
                    Text(
                        state.titleError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Type selector
                Text("Type", style = MaterialTheme.typography.labelMedium, color = ColorMuted)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("training" to "Training", "match" to "Match", "other" to "Other")
                        .forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = state.type == value,
                                onClick = { viewModel.setType(value) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = when (value) {
                                        "training" -> ColorTraining
                                        "match" -> ColorMatch
                                        else -> ColorOther
                                    }
                                )
                            ) {
                                Text(label)
                            }
                        }
                }

                // Teams multi-select
                Text("Team(s)", style = MaterialTheme.typography.labelMedium, color = ColorMuted)
                if (state.availableTeams.isEmpty()) {
                    Text(
                        "No teams available",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorMuted
                    )
                } else {
                    TeamChipGroup(
                        teams = state.availableTeams,
                        selectedTeamIds = state.selectedTeamIds,
                        onToggle = viewModel::toggleTeam
                    )
                }
                if (state.teamError != null) {
                    Text(
                        state.teamError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Time
            SectionHeader("Time")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start date
                DateRow(
                    label = "Start date",
                    date = state.startDate,
                    onClick = { showStartDatePicker = true }
                )
                // Start time + TZ label
                TimeRow(
                    label = "Start time",
                    time = state.startTime,
                    timezoneLabel = state.timezoneLabel,
                    onClick = { showStartTimePicker = true }
                )
                // End date
                DateRow(
                    label = "End date",
                    date = state.endDate,
                    onClick = { showEndDatePicker = true }
                )
                // End time
                TimeRow(
                    label = "End time",
                    time = state.endTime,
                    timezoneLabel = state.timezoneLabel,
                    onClick = { showEndTimePicker = true }
                )
                if (state.endTimeError != null) {
                    Text(
                        state.endTimeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Meetup time toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meetup time", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.meetupEnabled,
                        onCheckedChange = viewModel::toggleMeetup
                    )
                }
                if (state.meetupEnabled) {
                    TimeRow(
                        label = "Meetup at",
                        time = state.meetupTime,
                        timezoneLabel = state.timezoneLabel,
                        onClick = { showMeetupTimePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Location
            SectionHeader("Location")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = viewModel::setLocation,
                    label = { Text("Venue or address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Audience (ES-05 sub-group targeting)
            if (state.selectedTeamIds.isNotEmpty()) {
                SectionHeader("Audience")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Whole team",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorMuted
                    )
                    if (state.availableSubgroups.isNotEmpty()) {
                        SubgroupChipGroup(
                            subgroups = state.availableSubgroups,
                            selectedIds = state.selectedSubgroupIds,
                            onToggle = viewModel::toggleSubgroup
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 5: Options
            SectionHeader("Options")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Min attendees (ES-06)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Min attendees", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.minAttendeesEnabled,
                        onCheckedChange = viewModel::toggleMinAttendees
                    )
                }
                if (state.minAttendeesEnabled) {
                    OutlinedTextField(
                        value = if (state.minAttendees == 0) "" else state.minAttendees.toString(),
                        onValueChange = { text ->
                            val count = text.toIntOrNull() ?: 0
                            viewModel.setMinAttendees(count)
                        },
                        label = { Text("Min attendees") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Description
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 6: Recurring
            SectionHeader("Recurring")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring event", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.recurringEnabled,
                        onCheckedChange = viewModel::setRecurringEnabled
                    )
                }
                val recurringPattern = state.recurringPattern
                if (state.recurringEnabled && recurringPattern != null) {
                    val summary = buildRecurringSummary(recurringPattern)
                    FilterChip(
                        selected = true,
                        onClick = { showRecurringSheet = true },
                        label = { Text(summary) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        EventDatePickerDialog(
            initialDate = state.startDate,
            onDateSelected = { date ->
                viewModel.setStartDate(date)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showEndDatePicker) {
        EventDatePickerDialog(
            initialDate = state.endDate,
            onDateSelected = { date ->
                viewModel.setEndDate(date)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    // Time pickers
    if (showStartTimePicker) {
        EventTimePickerDialog(
            initialTime = state.startTime,
            onTimeSelected = { time ->
                viewModel.setStartTime(time)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        EventTimePickerDialog(
            initialTime = state.endTime,
            onTimeSelected = { time ->
                viewModel.setEndTime(time)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
    if (showMeetupTimePicker) {
        EventTimePickerDialog(
            initialTime = state.meetupTime,
            onTimeSelected = { time ->
                viewModel.setMeetupTime(time)
                showMeetupTimePicker = false
            },
            onDismiss = { showMeetupTimePicker = false }
        )
    }

    // Recurring pattern bottom sheet (S5)
    if (showRecurringSheet) {
        RecurringPatternSheet(
            initialPattern = state.recurringPattern ?: RecurringPatternState(),
            onDone = { pattern ->
                viewModel.setRecurringPattern(pattern)
                showRecurringSheet = false
            },
            onDismiss = {
                if (state.recurringPattern == null) {
                    viewModel.setRecurringEnabled(false)
                }
                showRecurringSheet = false
            }
        )
    }

    // Scope sheet (S6) for edit of recurring events
    if (showScopeSheet) {
        RecurringScopeSheet(
            mode = "edit",
            onContinue = { scope ->
                showScopeSheet = false
                viewModel.save(scope)
            },
            onDismiss = { showScopeSheet = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = ColorMuted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TeamChipGroup(
    teams: List<ch.teamorg.domain.MatchedTeam>,
    selectedTeamIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        teams.forEach { team ->
            FilterChip(
                selected = team.id in selectedTeamIds,
                onClick = { onToggle(team.id) },
                label = { Text(team.name) }
            )
        }
    }
}

@Composable
private fun SubgroupChipGroup(
    subgroups: List<ch.teamorg.domain.SubGroup>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        subgroups.forEach { subgroup ->
            FilterChip(
                selected = subgroup.id in selectedIds,
                onClick = { onToggle(subgroup.id) },
                label = { Text(subgroup.name) }
            )
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = ColorMuted)
            Text("${date.dayOfMonth} ${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.year}")
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime,
    timezoneLabel: String,
    onClick: () -> Unit
) {
    val hour = time.hour.toString().padStart(2, '0')
    val min = time.minute.toString().padStart(2, '0')
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = ColorMuted)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$hour:$min")
                Text(timezoneLabel, style = MaterialTheme.typography.bodySmall, color = ColorMuted)
            }
        }
    }
}

@Composable
private fun CtaBar(
    isEditMode: Boolean,
    isSaving: Boolean,
    isSeriesEvent: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(2f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditMode) "Save Changes" else "Create Event")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = run {
            val epochDays = initialDate.toEpochDays()
            epochDays * 24L * 60 * 60 * 1000
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun buildRecurringSummary(pattern: RecurringPatternState): String {
    val typePart = when (pattern.patternType) {
        "daily" -> "Daily"
        "weekly" -> {
            if (pattern.weekdays.isEmpty()) {
                "Weekly"
            } else {
                val days = pattern.weekdays.sorted().joinToString(", ") { dayIndex ->
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[dayIndex]
                }
                "Weekly on $days"
            }
        }
        else -> "Every ${pattern.intervalDays} days"
    }
    val endPart = if (pattern.hasEndDate && pattern.endDate != null) {
        " · Until ${pattern.endDate.dayOfMonth} ${pattern.endDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
    } else ""
    return "$typePart$endPart"
}
