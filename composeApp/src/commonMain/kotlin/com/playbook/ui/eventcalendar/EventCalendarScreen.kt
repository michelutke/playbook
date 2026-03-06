package com.playbook.ui.eventcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.playbook.domain.Event
import com.playbook.domain.EventStatus
import com.playbook.ui.components.EventTypeDot
import com.playbook.ui.components.color
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun EventCalendarScreen(
    teamId: String?,
    onNavigateBack: (() -> Unit)?,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToList: (String?) -> Unit,
    viewModel: EventCalendarViewModel = kmpViewModel(key = teamId ?: "all") { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventCalendarEvent.NavigateToDetail -> onNavigateToDetail(event.eventId)
                EventCalendarEvent.NavigateToList -> onNavigateToList(teamId)
            }
        }
    }
    EventCalendarContent(state = state, onAction = viewModel::submitAction, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCalendarContent(
    state: EventCalendarScreenState,
    onAction: (EventCalendarAction) -> Unit,
    onNavigateBack: (() -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(EventCalendarAction.OpenList) }) {
                        Icon(Icons.Outlined.FormatListBulleted, contentDescription = "List view")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                CalendarView.entries.forEachIndexed { index, view ->
                    SegmentedButton(
                        selected = state.view == view,
                        onClick = { onAction(EventCalendarAction.ViewToggled(view)) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalendarView.entries.size),
                        label = { Text(if (view == CalendarView.MONTH) "Month" else "Week") },
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                state.view == CalendarView.MONTH -> MonthView(state, onAction)
                else -> WeekView(state, onAction)
            }
        }
    }
}

@Composable
private fun MonthView(state: EventCalendarScreenState, onAction: (EventCalendarAction) -> Unit) {
    val monthStart = state.focusedMonthStart
    val daysInMonth = daysInMonth(monthStart.year, monthStart.monthNumber)
    val firstDayOffset = monthStart.dayOfWeek.ordinal
    val eventsByDate = state.events.groupBy { event ->
        event.startAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onAction(EventCalendarAction.MonthNavigated(false)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
            }
            val monthName = monthStart.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            Text(
                text = "$monthName ${monthStart.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onAction(EventCalendarAction.MonthNavigated(true)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            DAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - firstDayOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).height(52.dp))
                    } else {
                        val date = LocalDate(monthStart.year, monthStart.monthNumber, dayNum)
                        val eventsOnDay = eventsByDate[date] ?: emptyList()
                        val isSelected = state.selectedDate == date
                        DayCell(
                            dayNum = dayNum,
                            events = eventsOnDay,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(EventCalendarAction.DateSelected(date)) },
                        )
                    }
                }
            }
        }

        if (state.selectedDate != null) {
            val dayEvents = eventsByDate[state.selectedDate] ?: emptyList()
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (dayEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No events", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(dayEvents, key = { it.id }) { event ->
                        CalendarEventRow(event = event, onClick = { onAction(EventCalendarAction.EventSelected(event.id)) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNum: Int,
    events: List<Event>,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(
                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayNum.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        val types = events.map { it.type }.distinct().take(3)
        if (types.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                types.forEach { type ->
                    val cancelled = events.filter { it.type == type }.all { it.status == EventStatus.CANCELLED }
                    EventTypeDot(type = type, modifier = if (cancelled) Modifier.background(Color.Gray, CircleShape) else Modifier)
                }
            }
        }
    }
}

@Composable
private fun WeekView(state: EventCalendarScreenState, onAction: (EventCalendarAction) -> Unit) {
    val weekStart = state.focusedWeekStart
    val tz = TimeZone.currentSystemDefault()
    val eventsByDate = state.events.groupBy { event ->
        event.startAt.toLocalDateTime(tz).date
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onAction(EventCalendarAction.WeekNavigated(false)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous week")
            }
            val weekEnd = weekStart.plus(DatePeriod(days = 6))
            Text(
                text = "${weekStart.dayOfMonth} ${weekStart.month.name.take(3)} – ${weekEnd.dayOfMonth} ${weekEnd.month.name.take(3)} ${weekEnd.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onAction(EventCalendarAction.WeekNavigated(true)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next week")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            repeat(7) { col ->
                val date = weekStart.plus(DatePeriod(days = col))
                val isSelected = state.selectedDate == date
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAction(EventCalendarAction.DateSelected(date)) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        DAY_LABELS[col].take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        val focusDay = state.selectedDate ?: weekStart
        val dayEvents = eventsByDate[focusDay] ?: emptyList()
        if (dayEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No events", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(dayEvents, key = { it.id }) { event ->
                    CalendarEventRow(event = event, onClick = { onAction(EventCalendarAction.EventSelected(event.id)) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CalendarEventRow(event: Event, onClick: () -> Unit) {
    val tz = TimeZone.currentSystemDefault()
    val startLocal = event.startAt.toLocalDateTime(tz)
    val cancelled = event.status == EventStatus.CANCELLED
    val h = startLocal.hour.toString().padStart(2, '0')
    val m = startLocal.minute.toString().padStart(2, '0')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(CircleShape)
                .background(if (cancelled) Color.Gray else event.type.color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (cancelled) TextDecoration.LineThrough else null,
            )
            Text(
                text = "$h:$m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (event.seriesId != null) {
            Text("⟳", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}
