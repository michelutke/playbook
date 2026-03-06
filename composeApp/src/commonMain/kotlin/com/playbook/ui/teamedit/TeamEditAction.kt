package com.playbook.ui.teamedit

sealed class TeamEditAction {
    data class NameChanged(val name: String) : TeamEditAction()
    data class DescChanged(val desc: String) : TeamEditAction()
    data object Save : TeamEditAction()
}
