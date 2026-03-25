package ch.teamorg.ui.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.MatchedTeam
import ch.teamorg.ui.attendance.AttendanceRsvpButtons
import ch.teamorg.ui.attendance.BegrundungSheet
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// Event type colours
private val ColorTraining = Color(0xFF4F8EF7)
private val ColorMatch = Color(0xFF22C55E)
private val ColorOther = Color(0xFFA855F7)
private val ColorCancelled = Color(0xFFEF4444)
private val ColorCancelledGrey = Color(0xFF6B7280)

private fun eventTypeColor(type: String): Color = when (type) {
    "training" -> ColorTraining
    "match" -> ColorMatch
    else -> ColorOther
}

private fun calendarEventColor(type: String, isCancelled: Boolean): Color = when {
    isCancelled -> ColorCancelledGrey
    type == "training" -> ColorTraining
    type == "match" -> ColorMatch
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

private fun formatTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "$hour:$min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    viewModel: EventListViewModel,
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showBegrundung by remember { mutableStateOf(false) }
    var begrundungEventId by remember { mutableStateOf("") }
    var begrundungStatus by remember { mutableStateOf("unsure") }

    BegrundungSheet(
        visible = showBegrundung,
        mode = begrundungStatus,
        onDismiss = { showBegrundung = false },
        onConfirm = { reason ->
            showBegrundung = false
            viewModel.submitResponse(begrundungEventId, begrundungStatus, reason.ifBlank { null })
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                actions = {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        SegmentedButton(
                            selected = state.viewMode == EventViewMode.LIST,
                            onClick = { viewModel.setViewMode(EventViewMode.LIST) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("List") }
                        SegmentedButton(
                            selected = state.viewMode == EventViewMode.CALENDAR,
                            onClick = { viewModel.setViewMode(EventViewMode.CALENDAR) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Calendar") }
                    }
                }
            )
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
            // Filters: two rows
            FilterRows(
                teams = state.teams,
                selectedTeamIds = state.selectedTeamIds,
                selectedTypes = state.selectedTypes,
                onTeamToggle = viewModel::toggleTeamFilter,
                onTeamsClear = viewModel::clearTeamFilters,
                onTypeToggle = viewModel::toggleTypeFilter,
                onTypesClear = viewModel::clearTypeFilters
            )

            when (state.viewMode) {
                EventViewMode.LIST -> EventListContent(
                    state = state,
                    onEventClick = onEventClick,
                    onCreateClick = onCreateClick,
                    onRsvpSelect = { eventId, status ->
                        if (status == "unsure" || status == "declined") {
                            begrundungEventId = eventId
                            begrundungStatus = status
                            showBegrundung = true
                        } else {
                            viewModel.submitResponse(eventId, status, null)
                        }
                    }
                )
                EventViewMode.CALENDAR -> CalendarContent(
                    state = state,
                    viewModel = viewModel,
                    onEventClick = onEventClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRows(
    teams: List<MatchedTeam>,
    selectedTeamIds: Set<String>,
    selectedTypes: Set<String>,
    onTeamToggle: (String) -> Unit,
    onTeamsClear: () -> Unit,
    onTypeToggle: (String) -> Unit,
    onTypesClear: () -> Unit
) {
    Column {
        // Row 1: Team filters
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTeamIds.isEmpty(),
                    onClick = onTeamsClear,
                    label = { Text("All Teams") }
                )
            }
            items(teams) { team ->
                FilterChip(
                    selected = team.id in selectedTeamIds,
                    onClick = { onTeamToggle(team.id) },
                    label = { Text(team.name) }
                )
            }
        }
        // Row 2: Type filters
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTypes.isEmpty(),
                    onClick = onTypesClear,
                    label = { Text("All") }
                )
            }
            val types = listOf("training" to "Training", "match" to "Match", "other" to "Other")
            items(types) { (value, label) ->
                FilterChip(
                    selected = value in selectedTypes,
                    onClick = { onTypeToggle(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun EventListContent(
    state: EventListState,
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onRsvpSelect: (String, String) -> Unit  // (eventId, status)
) {
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
                    val counts = state.attendanceCounts[ewt.event.id]
                    EventListItem(
                        ewt = ewt,
                        confirmedCount = counts?.confirmedCount ?: 0,
                        maybeCount = counts?.maybeCount ?: 0,
                        declinedCount = counts?.declinedCount ?: 0,
                        myResponse = counts?.myResponse,
                        onClick = { onEventClick(ewt.event.id) },
                        onRsvpSelect = { status ->
                            onRsvpSelect(ewt.event.id, status)
                        }
                    )
                }
            }
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

private val AutoDeclinedBadgeBg = Color(0xFF6B7280)

@Composable
private fun EventListItem(
    ewt: EventWithTeams,
    confirmedCount: Int,
    maybeCount: Int,
    declinedCount: Int,
    myResponse: String?,
    onClick: () -> Unit,
    onRsvpSelect: (String) -> Unit = {}
) {
    val event = ewt.event
    val isCancelled = event.status == "cancelled"
    val isAutoDeclined = myResponse == "declined-auto"
    val typeColor = eventTypeColor(event.type)
    val startLocal = event.startAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = event.endAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val isMultiDay = startLocal.date != endLocal.date

    val cardAlpha = when {
        isAutoDeclined -> 0.6f
        isCancelled -> 0.4f
        else -> 1f
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 40.dp)
                        .padding(end = 0.dp),
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
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
                        if (isAutoDeclined) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = AutoDeclinedBadgeBg.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Auto-declined",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
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

                Column(horizontalAlignment = Alignment.End) {
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

            // Compact RSVP buttons row
            Spacer(modifier = Modifier.height(8.dp))
            AttendanceRsvpButtons(
                currentResponse = myResponse,
                confirmedCount = confirmedCount,
                maybeCount = maybeCount,
                declinedCount = declinedCount,
                deadlinePassed = false,
                compact = true,
                onSelect = onRsvpSelect
            )
        }
    }
}

// ── Calendar view ──────────────────────────────────────────

@Composable
private fun CalendarContent(
    state: EventListState,
    viewModel: EventListViewModel,
    onEventClick: (String) -> Unit
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    MonthView(state = state, viewModel = viewModel, onEventClick = onEventClick)
}

@Composable
private fun MonthView(
    state: EventListState,
    viewModel: EventListViewModel,
    onEventClick: (String) -> Unit
) {
    val currentDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    var displayYear by remember { mutableIntStateOf(currentDate.year) }
    var displayMonth by remember { mutableIntStateOf(currentDate.monthNumber) }
    // Track direction for animation
    var navDirection by remember { mutableIntStateOf(0) }

    // Swipe helpers
    fun goNext() {
        navDirection = 1
        if (displayMonth == 12) { displayMonth = 1; displayYear++ } else displayMonth++
    }
    fun goPrev() {
        navDirection = -1
        if (displayMonth == 1) { displayMonth = 12; displayYear-- } else displayMonth--
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Month navigation: ‹ March 2025 ›
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                color = Color(0xFF9090B0),
                fontSize = 22.sp,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { goPrev() }
            )
            Text(
                "${Month(displayMonth).name.lowercase().replaceFirstChar { it.uppercase() }} $displayYear",
                color = Color(0xFFF0F0FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "›",
                color = Color(0xFF9090B0),
                fontSize = 22.sp,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { goNext() }
            )
        }

        // Day headers
        DaysOfWeekHeader(daysOfWeek = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        ))

        // Calendar grid with swipe + animation
        val monthKey = displayYear * 100 + displayMonth
        AnimatedContent(
            targetState = monthKey,
            transitionSpec = {
                val dir = navDirection
                (slideInHorizontally { w -> if (dir >= 0) w else -w } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (dir >= 0) -w else w } + fadeOut())
            },
            modifier = Modifier.pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 100f) goPrev()
                        else if (totalDrag < -100f) goNext()
                    },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                )
            }
        ) { key ->
            val year = key / 100
            val month = key % 100
            val weeks = buildMonthGrid(year, month)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    ) {
                        week.forEach { date ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (date != null && date.monthNumber == month) {
                                    DayCell(
                                        date = date,
                                        events = state.eventsByDate[date] ?: emptyList(),
                                        isToday = date == currentDate,
                                        isSelected = date == state.selectedDate,
                                        onClick = { viewModel.selectDate(date) }
                                    )
                                } else if (date != null) {
                                    DayCell(
                                        date = date,
                                        events = emptyList(),
                                        isToday = false,
                                        isSelected = false,
                                        isOutside = true,
                                        onClick = {}
                                    )
                                } else {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        // Padding before divider
        Spacer(modifier = Modifier.height(12.dp))

        // Selected day events
        Column(modifier = Modifier.animateContentSize()) {
            if (state.selectedDate != null && state.selectedDayEvents.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF2A2A40))
                DayEventsList(events = state.selectedDayEvents, onEventClick = onEventClick)
            }
        }
    }
}

private fun buildMonthGrid(year: Int, month: Int): List<List<LocalDate?>> {
    val firstDay = LocalDate(year, month, 1)
    // Monday=1 .. Sunday=7
    val startDow = firstDay.dayOfWeek.ordinal + 1 // Monday=1 .. Sunday=7
    val daysInMonth = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    val cells = mutableListOf<LocalDate?>()
    // Pad start
    for (i in 1 until startDow) cells.add(null)
    for (d in 1..daysInMonth) cells.add(LocalDate(year, month, d))
    // Pad end to fill last week
    while (cells.size % 7 != 0) cells.add(null)

    return cells.chunked(7)
}

// V1 calendar color tokens
private val CalDayText = Color(0xFFC0C0D8)
private val CalDayOutside = Color(0xFF484860)
private val CalHeaderText = Color(0xFF606080)
private val CalChipTrainingBg = Color(0xFF162340)
private val CalChipMatchBg = Color(0xFF0A1E0E)
private val CalChipOtherBg = Color(0xFF1C1030)
private val CalChipCancelledBg = Color(0xFF1C1C2E)
private val CalSelectedBg = Color(0xFF4F8EF7)

private fun chipBgForType(type: String, isCancelled: Boolean): Color = when {
    isCancelled -> CalChipCancelledBg
    type == "training" -> CalChipTrainingBg
    type == "match" -> CalChipMatchBg
    else -> CalChipOtherBg
}

private fun chipTextForType(type: String, isCancelled: Boolean): Color = when {
    isCancelled -> ColorCancelledGrey
    type == "training" -> ColorTraining
    type == "match" -> ColorMatch
    else -> ColorOther
}

private fun chipLabel(type: String, title: String): String = when (type) {
    "training" -> "Train."
    "match" -> title.take(8)
    else -> title.take(6)
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                text = dayOfWeek.name.take(3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = CalHeaderText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    events: List<EventWithTeams>,
    isToday: Boolean,
    isSelected: Boolean,
    isOutside: Boolean = false,
    onClick: () -> Unit
) {
    val dayNumber = date.dayOfMonth.toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isOutside,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .alpha(if (isOutside) 0.3f else 1f)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Day number: selected = filled blue, today (not selected) = border only, default = plain
        when {
            isSelected -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CalSelectedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(dayNumber, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            isToday -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CalSelectedBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(dayNumber, color = CalSelectedBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        dayNumber,
                        color = if (!isOutside) CalDayText else CalDayOutside,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Event indicators
        if (events.isNotEmpty() && !isOutside) {
            Spacer(modifier = Modifier.height(2.dp))
            if (events.size <= 3) {
                // Show info chips
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    events.forEach { ewt ->
                        val isCancelled = ewt.event.status == "cancelled"
                        EventChip(
                            label = chipLabel(ewt.event.type, ewt.event.title),
                            bgColor = chipBgForType(ewt.event.type, isCancelled),
                            textColor = chipTextForType(ewt.event.type, isCancelled)
                        )
                    }
                }
            } else {
                // Fallback to dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    events.take(3).forEach { ewt ->
                        val isCancelled = ewt.event.status == "cancelled"
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(calendarEventColor(ewt.event.type, isCancelled))
                        )
                    }
                    Text("+${events.size - 3}", fontSize = 7.sp, color = CalHeaderText)
                }
            }
        }
    }
}

@Composable
private fun EventChip(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun DayEventsList(
    events: List<EventWithTeams>,
    onEventClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        events.forEach { ewt ->
            val isCancelled = ewt.event.status == "cancelled"
            val typeColor = calendarEventColor(ewt.event.type, isCancelled)
            Card(
                onClick = { onEventClick(ewt.event.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .alpha(if (isCancelled) 0.4f else 1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(typeColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = ewt.event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatTime(ewt.event.startAt)} – ${formatTime(ewt.event.endAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

