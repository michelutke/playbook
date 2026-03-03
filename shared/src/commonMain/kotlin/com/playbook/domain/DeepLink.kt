package com.playbook.domain

sealed class DeepLinkDestination {
    data class EventDetail(val eventId: String) : DeepLinkDestination()
    data class TeamDetail(val teamId: String) : DeepLinkDestination()
    data class EventAttendance(val eventId: String) : DeepLinkDestination()
    data class PlayerProfile(val playerId: String) : DeepLinkDestination()
    object NotificationInbox : DeepLinkDestination()
    object Unknown : DeepLinkDestination()
}

object DeepLinkRouter {
    private val eventPattern = Regex("^/events/([^/]+)$")
    private val teamPattern = Regex("^/teams/([^/]+)$")
    private val eventAttendancePattern = Regex("^/events/([^/]+)/attendance$")
    private val playerPattern = Regex("^/players/([^/]+)$")

    fun resolve(link: String): DeepLinkDestination {
        eventAttendancePattern.find(link)?.let { return DeepLinkDestination.EventAttendance(it.groupValues[1]) }
        eventPattern.find(link)?.let { return DeepLinkDestination.EventDetail(it.groupValues[1]) }
        teamPattern.find(link)?.let { return DeepLinkDestination.TeamDetail(it.groupValues[1]) }
        playerPattern.find(link)?.let { return DeepLinkDestination.PlayerProfile(it.groupValues[1]) }
        if (link == "/notifications") return DeepLinkDestination.NotificationInbox
        return DeepLinkDestination.Unknown
    }
}
