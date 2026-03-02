package com.playbook.android.ui.inviteaccept

import com.playbook.domain.InviteContext

data class InviteAcceptScreenState(
    val context: InviteContext? = null,
    val isLoading: Boolean = true,
    val isAccepting: Boolean = false,
    val error: String? = null,
)
