package com.playbook.ui.teamsetup

sealed class TeamSetupAction {
    data class NameChanged(val name: String) : TeamSetupAction()
    data class DescChanged(val desc: String) : TeamSetupAction()
    data object Submit : TeamSetupAction()
}
