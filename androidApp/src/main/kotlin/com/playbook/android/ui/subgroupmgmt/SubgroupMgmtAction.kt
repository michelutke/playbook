package com.playbook.android.ui.subgroupmgmt

sealed class SubgroupMgmtAction {
    data object Refresh : SubgroupMgmtAction()
    data class EditSelected(val subgroupId: String) : SubgroupMgmtAction()
    data object CreateSelected : SubgroupMgmtAction()
    data object DismissSheet : SubgroupMgmtAction()
    data class SheetNameChanged(val name: String) : SubgroupMgmtAction()
    data object SubmitSheet : SubgroupMgmtAction()
    data class DeleteConfirmed(val subgroupId: String) : SubgroupMgmtAction()
}
