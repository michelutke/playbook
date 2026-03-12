package com.playbook.ui.eventdetail

import com.playbook.domain.Event
import com.playbook.domain.RecurringScope

enum class PendingEventAction { EDIT, CANCEL }

data class EventDetailScreenState(
    val event: Event? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showScopeSheet: Boolean = false,
    val pendingAction: PendingEventAction? = null,
    val showCancelDialog: Boolean = false,
    val pendingCancelScope: RecurringScope = RecurringScope.THIS_ONLY,
)
