package com.playbook.android.ui.eventlist

import com.playbook.domain.Event
import com.playbook.domain.EventType
import com.playbook.domain.TeamRef

data class EventListScreenState(
    val events: List<Event> = emptyList(),
    val availableTeams: List<TeamRef> = emptyList(),
    val selectedType: EventType? = null,
    val selectedTeamId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
