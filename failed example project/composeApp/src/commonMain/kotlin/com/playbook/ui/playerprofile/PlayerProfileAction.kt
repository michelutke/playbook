package com.playbook.ui.playerprofile

sealed class PlayerProfileAction {
    data object Refresh : PlayerProfileAction()
    data object AddCoachRole : PlayerProfileAction()
    data object RemoveCoachRole : PlayerProfileAction()
    data object RemoveFromTeam : PlayerProfileAction()
}
