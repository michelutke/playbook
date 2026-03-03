package com.playbook.android.ui.stats

import com.playbook.domain.AttendanceRow
import com.playbook.domain.AttendanceStats
import com.playbook.domain.EventType
import kotlinx.datetime.Instant

data class TeamStatsScreenState(
    val rows: List<AttendanceRow> = emptyList(),
    val userStats: List<Pair<String, AttendanceStats>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedEventType: EventType? = null,
    val from: Instant? = null,
    val to: Instant? = null,
)
