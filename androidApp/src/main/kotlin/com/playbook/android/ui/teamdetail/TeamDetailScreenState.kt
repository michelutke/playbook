package com.playbook.android.ui.teamdetail

import com.playbook.domain.RosterMember
import com.playbook.domain.Team

enum class TeamDetailTab { ROSTER, SUB_GROUPS, SETTINGS }

data class TeamDetailScreenState(
    val team: Team? = null,
    val coaches: List<RosterMember> = emptyList(),
    val players: List<RosterMember> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: TeamDetailTab = TeamDetailTab.ROSTER,
    val isLoading: Boolean = true,
    val error: String? = null,
)
