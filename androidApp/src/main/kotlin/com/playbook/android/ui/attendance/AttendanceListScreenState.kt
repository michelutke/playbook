package com.playbook.android.ui.attendance

import com.playbook.domain.TeamAttendanceView

data class AttendanceListScreenState(
    val teamView: TeamAttendanceView? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedUserId: String? = null,
)
