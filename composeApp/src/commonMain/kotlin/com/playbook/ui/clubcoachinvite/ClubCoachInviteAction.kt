package com.playbook.ui.clubcoachinvite

sealed class ClubCoachInviteAction {
    data class EmailChanged(val email: String) : ClubCoachInviteAction()
    data object Send : ClubCoachInviteAction()
    data object CopyLink : ClubCoachInviteAction()
    data object RotateLink : ClubCoachInviteAction()
}
