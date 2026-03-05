package com.playbook.ui.attendancelist

sealed class AttendanceListAction {
    data object Refresh : AttendanceListAction()
    data class ToggleExpand(val userId: String) : AttendanceListAction()
}
