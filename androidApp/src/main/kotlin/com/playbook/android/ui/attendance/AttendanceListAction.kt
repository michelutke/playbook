package com.playbook.android.ui.attendance

sealed class AttendanceListAction {
    data object Refresh : AttendanceListAction()
    data class ToggleExpand(val userId: String) : AttendanceListAction()
}
