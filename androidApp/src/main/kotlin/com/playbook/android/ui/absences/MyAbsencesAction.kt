package com.playbook.android.ui.absences

import com.playbook.domain.AbwesenheitRule

sealed class MyAbsencesAction {
    data object Refresh : MyAbsencesAction()
    data object AddTapped : MyAbsencesAction()
    data class EditTapped(val rule: AbwesenheitRule) : MyAbsencesAction()
    data class DeleteTapped(val ruleId: String) : MyAbsencesAction()
    data object ConfirmDelete : MyAbsencesAction()
    data object DismissDeleteConfirm : MyAbsencesAction()
    data object SheetDismissed : MyAbsencesAction()
}
