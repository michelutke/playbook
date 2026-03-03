package com.playbook.android.ui.eventdetail

import com.playbook.domain.AttendanceResponseStatus
import com.playbook.domain.RecurringScope

sealed class EventDetailAction {
    data object Refresh : EventDetailAction()
    data object EditRequested : EventDetailAction()
    data object DuplicateRequested : EventDetailAction()
    data object CancelRequested : EventDetailAction()
    data class ScopeSelectedForEdit(val scope: RecurringScope) : EventDetailAction()
    data class ScopeSelectedForCancel(val scope: RecurringScope) : EventDetailAction()
    data object DismissScopeSheet : EventDetailAction()
    data object ConfirmCancel : EventDetailAction()
    data object DismissCancelDialog : EventDetailAction()
    data class AttendanceResponseTapped(val status: AttendanceResponseStatus) : EventDetailAction()
    data class BegrundungSubmitted(val reason: String?) : EventDetailAction()
    data object BegrundungDismissed : EventDetailAction()
}
