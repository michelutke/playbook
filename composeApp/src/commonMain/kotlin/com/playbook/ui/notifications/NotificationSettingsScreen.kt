package com.playbook.ui.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.settings == null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Unable to load settings", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val settings = state.settings!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (state.pushPermissionDenied) {
                        PushPermissionBanner()
                    }

                    SectionHeader("General")

                    SettingRow(
                        label = "New events",
                        checked = settings.newEvents,
                        onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetNewEvents(it)) },
                    )
                    SettingRow(
                        label = "Event changes",
                        checked = settings.eventChanges,
                        onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetEventChanges(it)) },
                    )
                    SettingRow(
                        label = "Event cancellations",
                        checked = settings.eventCancellations,
                        onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetEventCancellations(it)) },
                    )
                    SettingRow(
                        label = "Event reminders",
                        checked = settings.reminders,
                        onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetReminders(it)) },
                    )
                    if (settings.reminders) {
                        LeadTimePicker(
                            label = "Reminder lead time",
                            options = listOf("2h" to "2h", "1d" to "1 day", "2d" to "2 days"),
                            selected = settings.reminderLeadTime,
                            onSelected = { viewModel.submitAction(NotificationSettingsAction.SetReminderLeadTime(it)) },
                        )
                    }

                    if (state.isCoach) {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader("Coach")

                        SettingRow(
                            label = "Attendance per response",
                            checked = settings.attendancePerResponse,
                            onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetAttendancePerResponse(it)) },
                        )
                        SettingRow(
                            label = "Attendance summary",
                            checked = settings.attendanceSummary,
                            onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetAttendanceSummary(it)) },
                        )
                        if (settings.attendanceSummary) {
                            LeadTimePicker(
                                label = "Summary lead time",
                                options = listOf("2h" to "2h", "1d" to "1 day", "2d" to "2 days"),
                                selected = settings.attendanceSummaryLeadTime,
                                onSelected = { viewModel.submitAction(NotificationSettingsAction.SetAttendanceSummaryLeadTime(it)) },
                            )
                        }
                        SettingRow(
                            label = "Absence changes",
                            checked = settings.abwesenheitChanges,
                            onCheckedChange = { viewModel.submitAction(NotificationSettingsAction.SetAbwesenheitChanges(it)) },
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeadTimePicker(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, displayLabel) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(displayLabel)
                }
            }
        }
    }
}

@Composable
private fun PushPermissionBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Enable notifications in device settings",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
