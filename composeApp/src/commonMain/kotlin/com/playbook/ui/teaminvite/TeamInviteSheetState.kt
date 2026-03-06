package com.playbook.ui.teaminvite

import com.playbook.domain.Invite
import com.playbook.domain.MemberRole

data class TeamInviteSheetState(
    val teamName: String = "",
    val email: String = "",
    val role: MemberRole = MemberRole.PLAYER,
    val pendingInvites: List<Invite> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
)
