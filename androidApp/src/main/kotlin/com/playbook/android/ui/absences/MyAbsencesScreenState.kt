package com.playbook.android.ui.absences

import com.playbook.domain.AbwesenheitRule

data class MyAbsencesScreenState(
    val rules: List<AbwesenheitRule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false,
    val editingRule: AbwesenheitRule? = null,
    val showDeleteConfirmFor: String? = null,
    val backfillSnackbar: BackfillSnackbarState = BackfillSnackbarState.Hidden,
)

sealed class BackfillSnackbarState {
    data object Hidden : BackfillSnackbarState()
    data object Pending : BackfillSnackbarState()
    data object Done : BackfillSnackbarState()
    data object Failed : BackfillSnackbarState()
}
