package com.playbook.ui.teaminvite

import com.playbook.domain.MemberRole

sealed class TeamInviteAction {
    data class EmailChanged(val email: String) : TeamInviteAction()
    data class RoleChanged(val role: MemberRole) : TeamInviteAction()
    data object Send : TeamInviteAction()
    data object CopyLink : TeamInviteAction()
    data class Revoke(val inviteId: String) : TeamInviteAction()
}
