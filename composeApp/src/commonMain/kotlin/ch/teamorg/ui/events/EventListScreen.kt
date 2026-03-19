package ch.teamorg.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.teamorg.domain.Event
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.MatchedTeam
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Event type colours
private val ColorTraining = Color(0xFF4F8EF7)
private val ColorMatch = Color(0xFF22C55E)
private val ColorOther = Color(0xFFA855F7)
private val ColorCancelled = Color(0xFFEF4444)

private fun eventTypeColor(type: String): Color = when (type) {
    "training" -> ColorTraining
    "match" -> ColorMatch
    else -> ColorOther
}

private fun eventTypeLabel(type: String): String = when (type) {
    "training" -> "Training"
    "match" -> "Match"
    else -> "Other"
}

private fun formatDate(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayName = local.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "$dayName, ${local.dayOfMonth} $month · $hour:$min"
}

private fun formatDateOnly(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayName = local.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$dayName ${local.dayOfMonth} $month"
}

private fun formatDateRange(start: Instant, end: Instant): String {
    val startLocal = start.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = end.toLocalDateTime(TimeZone.currentSystemDefault())
    return if (startLocal.date == endLocal.date) {
        formatDate(start)
    } else {
        "${formatDateOnly(start)} — ${formatDateOnly(end)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    viewModel: EventListViewModel,
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Events") })
        },
        floatingActionButton = {
            if (state.isCoach) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create event")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FilterChipRow(
                teams = state.teams,
                selectedTeamId = state.selectedTeamId,
                selectedType = state.selectedType,
                onTeamSelected = viewModel::setTeamFilter,
                onTypeSelected = viewModel::setTypeFilter
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = state.error ?: "Unknown error",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.events.isEmpty() -> {
                    EmptyEventsList(isCoach = state.isCoach, onCreateClick = onCreateClick)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.events, key = { it.event.id }) { ewt ->
                            EventListItem(
                                ewt = ewt,
                                onClick = { onEventClick(ewt.event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    teams: List<MatchedTeam>,
    selectedTeamId: String?,
    selectedType: String?,
    onTeamSelected: (String?) -> Unit,
    onTypeSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All Teams" chip
        item {
            FilterChip(
                selected = selectedTeamId == null,
                onClick = { onTeamSelected(null) },
                label = { Text("All Teams") }
            )
        }
        // Per-team chips
        items(teams) { team ->
            FilterChip(
                selected = selectedTeamId == team.id,
                onClick = { onTeamSelected(team.id) },
                label = { Text(team.name) }
            )
        }

        // Separator
        item {
            Text(
                text = "|",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Type chips
        val types = listOf(null to "All", "training" to "Training", "match" to "Match", "other" to "Other")
        items(types) { (value, label) ->
            FilterChip(
                selected = selectedType == value,
                onClick = { onTypeSelected(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun EmptyEventsList(
    isCoach: Boolean,
    onCreateClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isCoach) "No events yet" else "No upcoming events",
                style = MaterialTheme.typography.titleMedium
            )
            if (isCoach) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onCreateClick) {
                    Text("Create your first event")
                }
            }
        }
    }
}

@Composable
private fun EventListItem(
    ewt: EventWithTeams,
    onClick: () -> Unit
) {
    val event = ewt.event
    val isCancelled = event.status == "cancelled"
    val typeColor = eventTypeColor(event.type)
    val startLocal = event.startAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = event.endAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val isMultiDay = startLocal.date != endLocal.date

    val rowModifier = if (isCancelled) {
        Modifier.fillMaxWidth().alpha(0.4f)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        onClick = onClick,
        modifier = rowModifier.padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon indicator
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .padding(end = 0.dp),
            )
            Icon(
                imageVector = when (event.type) {
                    "training" -> Icons.Default.DateRange
                    "match" -> Icons.Default.DateRange
                    else -> Icons.Default.DateRange
                },
                contentDescription = eventTypeLabel(event.type),
                tint = typeColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCancelled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = ColorCancelled.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Cancelled",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorCancelled,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isMultiDay) formatDateRange(event.startAt, event.endAt)
                           else formatDate(event.startAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side badges
            Column(horizontalAlignment = Alignment.End) {
                // Multi-team badge
                if (ewt.matchedTeams.size > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${ewt.matchedTeams.size} teams",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Recurring indicator
                if (event.seriesId != null) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recurring",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
