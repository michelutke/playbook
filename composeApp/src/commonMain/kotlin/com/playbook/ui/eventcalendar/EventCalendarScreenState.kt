package com.playbook.ui.eventcalendar

import com.playbook.domain.Event
import kotlinx.datetime.LocalDate

enum class CalendarView { MONTH, WEEK }

data class EventCalendarScreenState(
    val events: List<Event> = emptyList(),
    val view: CalendarView = CalendarView.MONTH,
    val focusedMonthStart: LocalDate,
    val focusedWeekStart: LocalDate,
    val selectedDate: LocalDate? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
