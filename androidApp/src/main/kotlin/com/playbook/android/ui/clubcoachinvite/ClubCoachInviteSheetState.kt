package com.playbook.android.ui.clubcoachinvite

import com.playbook.domain.CoachLink

data class ClubCoachInviteSheetState(
    val clubName: String = "",
    val coachLink: CoachLink? = null,
    val email: String = "",
    val isSending: Boolean = false,
    val isCopied: Boolean = false,
    val error: String? = null,
)
