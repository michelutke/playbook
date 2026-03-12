package com.playbook.ui.eventcalendar

import kotlinx.datetime.LocalDate

sealed class EventCalendarAction {
    data object Refresh : EventCalendarAction()
    data class ViewToggled(val view: CalendarView) : EventCalendarAction()
    data class DateSelected(val date: LocalDate) : EventCalendarAction()
    data class MonthNavigated(val forward: Boolean) : EventCalendarAction()
    data class WeekNavigated(val forward: Boolean) : EventCalendarAction()
    data class EventSelected(val eventId: String) : EventCalendarAction()
    data object OpenList : EventCalendarAction()
}
