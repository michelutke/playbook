package ch.teamorg.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.teamorg.domain.EventWithTeams
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ColorTraining = Color(0xFF4F8EF7)
private val ColorMatch = Color(0xFF22C55E)
private val ColorOther = Color(0xFFA855F7)
private val ColorError = Color(0xFFEF4444)

private fun detailTypeColor(type: String): Color = when (type) {
    "training" -> ColorTraining
    "match" -> ColorMatch
    else -> ColorOther
}

private fun detailTypeLabel(type: String): String = when (type) {
    "training" -> "Training"
    "match" -> "Match"
    else -> "Other"
}

private fun formatDetailDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayName = local.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "$dayName, ${local.dayOfMonth} $month ${local.year} · $hour:$min"
}

private fun formatDetailTimeOnly(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "· $hour:$min"
}

private fun formatMeetupTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "Meetup: $hour:$min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.event?.event?.title ?: "Event",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isCoach) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = {
                                        showMenu = false
                                        onDuplicate()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel event", color = ColorError) },
                                    onClick = {
                                        showMenu = false
                                        onCancel()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Failed to load event",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            state.event != null -> {
                EventDetailContent(
                    ewt = state.event!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    ewt: EventWithTeams,
    modifier: Modifier = Modifier
) {
    val event = ewt.event
    val isCancelled = event.status == "cancelled"
    val startLocal = event.startAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = event.endAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val isMultiDay = startLocal.date != endLocal.date
    var descriptionExpanded by remember { mutableStateOf(false) }
    val eventLocation = event.location
    val eventDescription = event.description
    val eventMinAttendees = event.minAttendees
    val eventMeetupAt = event.meetupAt

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // Cancelled banner
        if (isCancelled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ColorError.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "This event has been cancelled",
                    color = ColorError,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Section 1: Header card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Type chip
                    Surface(
                        color = detailTypeColor(event.type).copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = detailTypeLabel(event.type),
                            style = MaterialTheme.typography.labelMedium,
                            color = detailTypeColor(event.type),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Recurring indicator
                    if (event.seriesId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recurring",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recurring",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 2: Time section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isMultiDay) {
                        Text(
                            text = formatDetailDateTime(event.startAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "— ${formatDetailDateTime(event.endAt)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = formatDetailDateTime(event.startAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatDetailTimeOnly(event.endAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Meetup time
                    if (eventMeetupAt != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMeetupTime(eventMeetupAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 3: Location (if set)
            if (eventLocation != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = eventLocation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* Open maps intent — platform specific */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open in Maps")
                        }
                    }
                }
            }

            // Section 4: Team / Sub-group section
            if (ewt.matchedTeams.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Teams",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ewt.matchedTeams.size == 1) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = ewt.matchedTeams.first().name,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${ewt.matchedTeams.size} teams",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Sub-group chips
                        if (event.subgroupIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                event.subgroupIds.forEach { subgroupId ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "⊂ $subgroupId",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Description (if set)
            if (eventDescription != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = eventDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Show "Show more" toggle only if description is likely truncated
                        val lineCount = eventDescription.count { it == '\n' } + 1
                        if (!descriptionExpanded && lineCount > 3) {
                            TextButton(
                                onClick = { descriptionExpanded = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Show more")
                            }
                        } else if (descriptionExpanded) {
                            TextButton(
                                onClick = { descriptionExpanded = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Show less")
                            }
                        }
                    }
                }
            }

            // Section 6: Min attendees (if set)
            if (eventMinAttendees != null) {
                Text(
                    text = "Min. attendees: $eventMinAttendees",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Section 7: Attendance placeholder card
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Attendance — Coming soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
