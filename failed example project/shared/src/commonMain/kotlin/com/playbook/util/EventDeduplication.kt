package com.playbook.util

import com.playbook.domain.Event
import com.playbook.domain.TeamRef

/**
 * ES-027: Groups events by event_id and merges matched_teams[].
 * Ensures a player in multiple targeted teams sees one entry per event.
 * Logic lives in shared domain, not in Composables.
 */
fun deduplicateEvents(events: List<Event>): List<Event> =
    events
        .groupBy { it.id }
        .map { (_, group) ->
            val merged = group.flatMap { it.matchedTeams }.distinctBy { it.id }
            group.first().copy(matchedTeams = merged)
        }

val Event.isMultiTeam: Boolean get() = matchedTeams.size > 1

val Event.displayTeams: List<TeamRef> get() = matchedTeams.ifEmpty { teams }
