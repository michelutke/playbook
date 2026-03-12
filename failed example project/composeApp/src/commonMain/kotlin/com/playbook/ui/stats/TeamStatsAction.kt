package com.playbook.ui.stats

import com.playbook.domain.EventType

sealed class TeamStatsAction {
    data object Refresh : TeamStatsAction()
    data class FilterByEventType(val eventType: EventType?) : TeamStatsAction()
}
