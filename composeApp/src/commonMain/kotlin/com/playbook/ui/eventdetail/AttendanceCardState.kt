package com.playbook.ui.eventdetail

import com.playbook.domain.AttendanceResponse
import com.playbook.domain.AttendanceResponseStatus

data class AttendanceCardState(
    val myResponse: AttendanceResponse? = null,
    val isLoading: Boolean = false,
    val isDeadlinePassed: Boolean = false,
    val showBegrundungSheet: Boolean = false,
    val pendingStatus: AttendanceResponseStatus? = null,
    val error: String? = null,
)
