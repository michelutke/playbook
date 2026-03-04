package com.playbook.ui.playerprofile

import com.playbook.domain.PlayerProfile
import com.playbook.domain.RosterMember

data class PlayerProfileScreenState(
    val member: RosterMember? = null,
    val profile: PlayerProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
