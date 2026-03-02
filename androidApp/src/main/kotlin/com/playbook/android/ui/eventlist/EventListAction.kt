package com.playbook.android.ui.eventlist

import com.playbook.domain.EventType

sealed class EventListAction {
    data object Refresh : EventListAction()
    data class TypeFilterChanged(val type: EventType?) : EventListAction()
    data class TeamFilterChanged(val teamId: String?) : EventListAction()
    data class EventSelected(val eventId: String) : EventListAction()
    data object CreateEvent : EventListAction()
    data object OpenCalendar : EventListAction()
}
