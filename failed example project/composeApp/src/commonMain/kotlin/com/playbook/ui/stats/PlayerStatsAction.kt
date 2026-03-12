package com.playbook.ui.stats

import com.playbook.domain.EventType

sealed class PlayerStatsAction {
    data object Refresh : PlayerStatsAction()
    data class FilterByEventType(val eventType: EventType?) : PlayerStatsAction()
}
