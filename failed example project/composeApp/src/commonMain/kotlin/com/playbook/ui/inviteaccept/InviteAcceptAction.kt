package com.playbook.ui.inviteaccept

sealed class InviteAcceptAction {
    data object Accept : InviteAcceptAction()
    data object Decline : InviteAcceptAction()
}
