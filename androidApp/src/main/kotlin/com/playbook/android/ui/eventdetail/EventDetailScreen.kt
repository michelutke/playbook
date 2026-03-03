package com.playbook.android.ui.eventdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playbook.android.ui.attendance.BegrundungSheet
import com.playbook.android.ui.components.EventTypeChip
import com.playbook.domain.AttendanceResponseStatus
import com.playbook.domain.Event
import com.playbook.domain.EventStatus
import com.playbook.domain.RecurringScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, RecurringScope) -> Unit,
    onNavigateToDuplicate: (String) -> Unit,
    viewModel: EventDetailViewModel = koinViewModel { parametersOf(eventId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val attendanceState by viewModel.attendanceState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EventDetailNavEvent.NavigateToEdit -> onNavigateToEdit(event.eventId, event.scope)
                is EventDetailNavEvent.NavigateToDuplicate -> onNavigateToDuplicate(event.eventId)
                EventDetailNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }
    EventDetailContent(
        state = state,
        attendanceState = attendanceState,
        onAction = viewModel::submitAction,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    state: EventDetailScreenState,
    attendanceState: AttendanceCardState,
    onAction: (EventDetailAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.event != null && state.event.status == EventStatus.ACTIVE) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { menuExpanded = false; onAction(EventDetailAction.EditRequested) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { menuExpanded = false; onAction(EventDetailAction.DuplicateRequested) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel Event", color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuExpanded = false; onAction(EventDetailAction.CancelRequested) },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            state.event != null -> EventDetailBody(
                event = state.event,
                attendanceState = attendanceState,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (state.showScopeSheet) {
        RecurringScopeSheet(
            pendingAction = state.pendingAction,
            onScopeSelected = { scope ->
                when (state.pendingAction) {
                    PendingEventAction.EDIT -> onAction(EventDetailAction.ScopeSelectedForEdit(scope))
                    PendingEventAction.CANCEL -> onAction(EventDetailAction.ScopeSelectedForCancel(scope))
                    null -> onAction(EventDetailAction.DismissScopeSheet)
                }
            },
            onDismiss = { onAction(EventDetailAction.DismissScopeSheet) },
        )
    }

    if (attendanceState.showBegrundungSheet && attendanceState.pendingStatus != null) {
        BegrundungSheet(
            status = attendanceState.pendingStatus,
            onSubmit = { reason -> onAction(EventDetailAction.BegrundungSubmitted(reason)) },
            onDismiss = { onAction(EventDetailAction.BegrundungDismissed) },
        )
    }

    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { onAction(EventDetailAction.DismissCancelDialog) },
            title = { Text("Cancel Event") },
            text = { Text("Are you sure you want to cancel this event? Attendance records will be preserved.") },
            confirmButton = {
                Button(onClick = { onAction(EventDetailAction.ConfirmCancel) }) { Text("Cancel Event") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(EventDetailAction.DismissCancelDialog) }) { Text("Keep") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDetailBody(
    event: Event,
    attendanceState: AttendanceCardState,
    onAction: (EventDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tz = TimeZone.currentSystemDefault()
    val startLocal = event.startAt.toLocalDateTime(tz)
    val endLocal = event.endAt.toLocalDateTime(tz)
    val context = LocalContext.current
    val cancelled = event.status == EventStatus.CANCELLED

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            if (cancelled) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "This event has been cancelled",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            EventTypeChip(type = event.type)
            Spacer(Modifier.height(12.dp))

            // Date and time
            val sameDay = startLocal.date == endLocal.date
            val dateStr = if (sameDay) {
                "%d %s %d".format(startLocal.dayOfMonth, startLocal.month.name.take(3), startLocal.year)
            } else {
                "%s %d %s – %s %d %s".format(
                    startLocal.dayOfWeek.name.take(3),
                    startLocal.dayOfMonth,
                    startLocal.month.name.take(3),
                    endLocal.dayOfWeek.name.take(3),
                    endLocal.dayOfMonth,
                    endLocal.month.name.take(3),
                )
            }
            val timeStr = "%02d:%02d – %02d:%02d".format(startLocal.hour, startLocal.minute, endLocal.hour, endLocal.minute)
            DetailRow(label = "Date", value = dateStr)
            DetailRow(label = "Time", value = timeStr)

            event.meetupAt?.let { meetup ->
                val meetupLocal = meetup.toLocalDateTime(tz)
                DetailRow(label = "Meet up", value = "%02d:%02d".format(meetupLocal.hour, meetupLocal.minute))
            }

            if (event.seriesId != null) {
                DetailRow(label = "Recurring", value = "⟳ Yes")
            }

            Spacer(Modifier.height(8.dp))

            // Location
            val location = event.location
            if (!location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(location, style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("Open in Maps", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Teams
            if (event.teams.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Teams", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    event.teams.forEach { team ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                team.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // Subgroups
            if (event.subgroups.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Sub-groups", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    event.subgroups.forEach { sg ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                sg.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            // Min attendees
            event.minAttendees?.let {
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Min attendees", value = it.toString())
            }

            // Description
            val description = event.description
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }

            // Attendance card
            Spacer(Modifier.height(16.dp))
            MyAttendanceCard(attendanceState = attendanceState, onAction = onAction)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MyAttendanceCard(
    attendanceState: AttendanceCardState,
    onAction: (EventDetailAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Your attendance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            if (attendanceState.isDeadlinePassed) {
                Text(
                    "Deadline reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val currentStatus = attendanceState.myResponse?.status
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceResponseStatus.entries
                        .filter { it in listOf(AttendanceResponseStatus.CONFIRMED, AttendanceResponseStatus.DECLINED, AttendanceResponseStatus.UNSURE) }
                        .forEach { status ->
                            val isCurrent = currentStatus == status
                            val label = when (status) {
                                AttendanceResponseStatus.CONFIRMED -> "Confirmed"
                                AttendanceResponseStatus.DECLINED -> "Declined"
                                AttendanceResponseStatus.UNSURE -> "Unsure"
                                else -> ""
                            }
                            if (isCurrent) {
                                Button(
                                    onClick = { onAction(EventDetailAction.AttendanceResponseTapped(status)) },
                                    enabled = !attendanceState.isLoading,
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            } else {
                                OutlinedButton(
                                    onClick = { onAction(EventDetailAction.AttendanceResponseTapped(status)) },
                                    enabled = !attendanceState.isLoading,
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                }
            }
            if (attendanceState.error != null) {
                Spacer(Modifier.height(4.dp))
                Text(attendanceState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringScopeSheet(
    pendingAction: PendingEventAction?,
    onScopeSelected: (RecurringScope) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val title = if (pendingAction == PendingEventAction.CANCEL) "Cancel recurring event" else "Edit recurring event"
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            RecurringScope.entries.forEach { scope ->
                val label = when (scope) {
                    RecurringScope.THIS_ONLY -> "This event only"
                    RecurringScope.THIS_AND_FUTURE -> "This and future events"
                    RecurringScope.ALL -> "All events in series"
                }
                val description = when (scope) {
                    RecurringScope.THIS_ONLY -> "Only this occurrence will be affected"
                    RecurringScope.THIS_AND_FUTURE -> "This and all future occurrences will be affected"
                    RecurringScope.ALL -> "All past and future occurrences will be affected"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = false, onClick = { onScopeSelected(scope) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
