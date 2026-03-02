package com.playbook.android.ui.eventlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playbook.android.ui.components.color
import com.playbook.android.ui.components.icon
import com.playbook.android.ui.components.label
import com.playbook.domain.Event
import com.playbook.domain.EventStatus
import com.playbook.domain.EventType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EventListScreen(
    teamId: String?,
    onNavigateBack: (() -> Unit)?,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: (String?) -> Unit,
    onNavigateToCalendar: (String?) -> Unit,
    viewModel: EventListViewModel = koinViewModel { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventListEvent.NavigateToDetail -> onNavigateToDetail(event.eventId)
                EventListEvent.NavigateToCreate -> onNavigateToCreate(teamId)
                EventListEvent.NavigateToCalendar -> onNavigateToCalendar(teamId)
            }
        }
    }
    EventListContent(state = state, onAction = viewModel::submitAction, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventListContent(
    state: EventListScreenState,
    onAction: (EventListAction) -> Unit,
    onNavigateBack: (() -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(EventListAction.OpenCalendar) }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar view")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(EventListAction.CreateEvent) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Create event")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipsRow(state, onAction)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onAction(EventListAction.Refresh) }) { Text("Retry") }
                    }
                }
                state.events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.events, key = { it.id }) { event ->
                        EventListItem(event = event, onClick = { onAction(EventListAction.EventSelected(event.id)) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(state: EventListScreenState, onAction: (EventListAction) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.availableTeams.forEach { team ->
            FilterChip(
                selected = state.selectedTeamId == team.id,
                onClick = { onAction(EventListAction.TeamFilterChanged(if (state.selectedTeamId == team.id) null else team.id)) },
                label = { Text(team.name) },
            )
        }
        FilterChip(
            selected = state.selectedType == null,
            onClick = { onAction(EventListAction.TypeFilterChanged(null)) },
            label = { Text("All") },
        )
        EventType.entries.forEach { type ->
            FilterChip(
                selected = state.selectedType == type,
                onClick = { onAction(EventListAction.TypeFilterChanged(type)) },
                label = { Text(type.label) },
            )
        }
    }
}

@Composable
private fun EventListItem(event: Event, onClick: () -> Unit) {
    val tz = TimeZone.currentSystemDefault()
    val startLocal = event.startAt.toLocalDateTime(tz)
    val cancelled = event.status == EventStatus.CANCELLED
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (cancelled) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                else Modifier
            ),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = event.title,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (event.seriesId != null) {
                    Text("⟳", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        supportingContent = {
            val dateStr = "%d %s %02d:%02d".format(
                startLocal.dayOfMonth,
                startLocal.month.name.take(3),
                startLocal.hour,
                startLocal.minute,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                if (event.teams.isNotEmpty()) {
                    Text("·", style = MaterialTheme.typography.bodySmall)
                    Text(
                        event.teams.first().name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (event.teams.size > 1) {
                        Text("+${event.teams.size - 1}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(event.type.color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(event.type.icon, style = MaterialTheme.typography.titleMedium)
            }
        },
        trailingContent = if (cancelled) {
            {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        "Cancelled",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        } else null,
    )
}
