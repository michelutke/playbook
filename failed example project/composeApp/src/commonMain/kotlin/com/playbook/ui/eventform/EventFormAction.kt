package com.playbook.ui.eventform

import com.playbook.domain.EventType
import com.playbook.domain.PatternType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class EventFormAction {
    data class TitleChanged(val value: String) : EventFormAction()
    data class TypeSelected(val type: EventType) : EventFormAction()
    data class StartDateChanged(val date: LocalDate) : EventFormAction()
    data class StartTimeChanged(val time: LocalTime) : EventFormAction()
    data class EndDateChanged(val date: LocalDate) : EventFormAction()
    data class EndTimeChanged(val time: LocalTime) : EventFormAction()
    data class MeetupTimeChanged(val time: LocalTime?) : EventFormAction()
    data class LocationChanged(val value: String) : EventFormAction()
    data class DescriptionChanged(val value: String) : EventFormAction()
    data class MinAttendeesChanged(val value: String) : EventFormAction()
    data class TeamToggled(val teamId: String) : EventFormAction()
    data class SubgroupToggled(val subgroupId: String) : EventFormAction()
    data class RecurringToggled(val enabled: Boolean) : EventFormAction()
    data class PatternTypeSelected(val patternType: PatternType) : EventFormAction()
    data class WeekdayToggled(val weekday: Int) : EventFormAction()
    data class IntervalDaysChanged(val value: String) : EventFormAction()
    data class SeriesEndDateChanged(val date: LocalDate?) : EventFormAction()
    data object PatternSheetDismissed : EventFormAction()
    data object Submit : EventFormAction()
}
