package ch.teamorg.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgPrimary = Color(0xFF090912)
private val MutedForeground = Color(0xFF9090B0)
private val PrimaryBlue = Color(0xFF4F8EF7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showReminderPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPrimary)
                .verticalScroll(rememberScrollState())
        ) {
            // Team picker
            if (state.teams.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.teams) { team ->
                        FilterChip(
                            selected = state.selectedTeamId == team.teamId,
                            onClick = { viewModel.selectTeam(team.teamId) },
                            label = { Text(team.teamName) }
                        )
                    }
                }
            }

            val settings = state.settings
            if (settings != null) {
                // Events section
                SectionHeader("Events")
                ToggleRow(
                    label = "New event",
                    checked = settings.eventsNew,
                    onCheckedChange = { viewModel.updateSetting("eventsNew", it) }
                )
                ToggleRow(
                    label = "Event changes",
                    checked = settings.eventsEdit,
                    onCheckedChange = { viewModel.updateSetting("eventsEdit", it) }
                )
                ToggleRow(
                    label = "Cancellations",
                    checked = settings.eventsCancel,
                    onCheckedChange = { viewModel.updateSetting("eventsCancel", it) }
                )

                // Reminders section
                SectionHeader("Reminders")
                ToggleRow(
                    label = "Event reminders",
                    checked = settings.remindersEnabled,
                    onCheckedChange = { viewModel.updateSetting("remindersEnabled", it) }
                )
                if (settings.remindersEnabled) {
                    LeadTimeRow(
                        leadMinutes = settings.reminderLeadMinutes,
                        onClick = { showReminderPicker = true }
                    )
                }

                // Responses section (coach only)
                if (state.isCoach) {
                    SectionHeader("Responses")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notify me",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        SegmentedButton(
                            selected = settings.coachResponseMode == "per_response",
                            onClick = { viewModel.updateSetting("coachResponseMode", "per_response") },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Per response") }
                        SegmentedButton(
                            selected = settings.coachResponseMode == "summary",
                            onClick = { viewModel.updateSetting("coachResponseMode", "summary") },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Pre-event summary") }
                    }

                    // Absences section (coach only)
                    SectionHeader("Absences")
                    ToggleRow(
                        label = "Absence notifications",
                        checked = settings.absencesEnabled,
                        onCheckedChange = { viewModel.updateSetting("absencesEnabled", it) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showReminderPicker) {
            val settings2 = state.settings
            ReminderPickerSheet(
                currentLeadMinutes = settings2?.reminderLeadMinutes,
                onConfirm = { minutes ->
                    viewModel.updateSetting("reminderLeadMinutes", minutes)
                    showReminderPicker = false
                },
                onRemove = null,
                onDismiss = { showReminderPicker = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MutedForeground,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun LeadTimeRow(leadMinutes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Default lead time",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text(
                text = formatLeadTime(leadMinutes),
                color = PrimaryBlue
            )
        }
    }
}

fun formatLeadTime(minutes: Int): String = when {
    minutes >= 1440 -> "${minutes / 1440} day(s) before"
    minutes >= 60 -> "${minutes / 60} hours before"
    else -> "$minutes min before"
}
