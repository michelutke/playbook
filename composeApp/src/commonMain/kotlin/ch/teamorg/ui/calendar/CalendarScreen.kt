package ch.teamorg.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.EventWithTeams
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// Event type colours
private val ColorTraining = Color(0xFF4F8EF7)
private val ColorMatch = Color(0xFF22C55E)
private val ColorOther = Color(0xFFA855F7)
private val ColorCancelled = Color(0xFF6B7280)

private fun eventTypeColor(type: String, isCancelled: Boolean): Color = when {
    isCancelled -> ColorCancelled
    type == "training" -> ColorTraining
    type == "match" -> ColorMatch
    else -> ColorOther
}

private fun formatTime(instant: kotlinx.datetime.Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "$hour:$min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = state.viewMode == CalendarViewMode.MONTH,
                            onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Month") }
                        SegmentedButton(
                            selected = state.viewMode == CalendarViewMode.WEEK,
                            onClick = { viewModel.setViewMode(CalendarViewMode.WEEK) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Week") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isCoach) {
                FloatingActionButton(
                    onClick = onCreateClick,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create event")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state.viewMode) {
                CalendarViewMode.MONTH -> MonthView(state, viewModel, onEventClick)
                CalendarViewMode.WEEK -> WeekView(state, onEventClick)
            }
        }
    }
}

@Composable
private fun MonthView(
    state: CalendarState,
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit
) {
    val currentDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val currentMonth = remember { currentDate.yearMonth }
    val startMonth = remember { currentMonth.minusMonths(6) }
    val endMonth = remember { currentMonth.plusMonths(6) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Days of week header
        DaysOfWeekHeader(daysOfWeek = daysOfWeek)

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                DayCell(
                    day = day,
                    events = state.eventsByDate[day.date] ?: emptyList(),
                    isToday = day.date == currentDate,
                    isSelected = day.date == state.selectedDate,
                    onClick = {
                        if (day.position == DayPosition.MonthDate) {
                            viewModel.selectDate(day.date)
                        }
                    }
                )
            }
        )

        // Selected day events list
        if (state.selectedDate != null && state.selectedDayEvents.isNotEmpty()) {
            HorizontalDivider()
            DayEventsList(events = state.selectedDayEvents, onEventClick = onEventClick)
        }
    }
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                text = dayOfWeek.name.take(3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    events: List<EventWithTeams>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = isCurrentMonth, onClick = onClick)
            .alpha(if (isCurrentMonth) 1f else 0.3f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Date number with today indicator
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isToday) TextDecoration.Underline else TextDecoration.None
                )
            }

            // Event dots (max 3)
            if (events.isNotEmpty() && isCurrentMonth) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.height(6.dp)
                ) {
                    val displayEvents = events.take(3)
                    displayEvents.forEach { ewt ->
                        val isCancelled = ewt.event.status == "cancelled"
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(eventTypeColor(ewt.event.type, isCancelled))
                        )
                    }
                    if (events.size > 3) {
                        Text(
                            text = "+${events.size - 3}",
                            fontSize = 7.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEventsList(
    events: List<EventWithTeams>,
    onEventClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(events, key = { it.event.id }) { ewt ->
            val isCancelled = ewt.event.status == "cancelled"
            val typeColor = eventTypeColor(ewt.event.type, isCancelled)
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

@Composable
private fun WeekView(
    state: CalendarState,
    onEventClick: (String) -> Unit
) {
    val currentDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val startDate = remember { currentDate.minus(180, DateTimeUnit.DAY) }
    val endDate = remember { currentDate.plus(180, DateTimeUnit.DAY) }

    val weekState = rememberWeekCalendarState(
        startDate = startDate,
        endDate = endDate,
        firstVisibleWeekDate = currentDate,
        firstDayOfWeek = DayOfWeek.MONDAY
    )

    WeekCalendar(
        state = weekState,
        dayContent = { day ->
            WeekDayColumn(
                day = day,
                events = state.eventsByDate[day.date] ?: emptyList(),
                isToday = day.date == currentDate,
                onEventClick = onEventClick
            )
        }
    )
}

@Composable
private fun WeekDayColumn(
    day: WeekDay,
    events: List<EventWithTeams>,
    isToday: Boolean,
    onEventClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day header
        Text(
            text = day.date.dayOfMonth.toString(),
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isToday) TextDecoration.Underline else TextDecoration.None
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Event blocks
        events.forEach { ewt ->
            val isCancelled = ewt.event.status == "cancelled"
            val typeColor = eventTypeColor(ewt.event.type, isCancelled)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .alpha(if (isCancelled) 0.4f else 1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor.copy(alpha = 0.15f))
                    .border(
                        width = 2.dp,
                        color = typeColor,
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                    )
                    .clickable { onEventClick(ewt.event.id) }
                    .padding(start = 6.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            ) {
                Column {
                    Text(
                        text = ewt.event.title,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTime(ewt.event.startAt),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

// Extension functions for YearMonth arithmetic
private fun YearMonth.minusMonths(months: Int): YearMonth {
    var year = this.year
    var month = this.monthNumber - months
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return YearMonth(year, month)
}

private fun YearMonth.plusMonths(months: Int): YearMonth {
    return this.minusMonths(-months)
}
