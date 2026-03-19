package ch.teamorg.domain

import kotlinx.serialization.Serializable

@Serializable
data class UserRoles(
    val clubRoles: List<ClubRoleEntry> = emptyList(),
    val teamRoles: List<TeamRoleEntry> = emptyList()
)

@Serializable
data class ClubRoleEntry(val clubId: String, val role: String)

@Serializable
data class TeamRoleEntry(val teamId: String, val clubId: String, val role: String)

@Serializable
data class Club(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val sportType: String
)

@Serializable
data class Team(
    val id: String,
    val clubId: String,
    val name: String,
    val memberCount: Int
)

@Serializable
data class TeamMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val jerseyNumber: Int?,
    val position: String?
)

@Serializable
data class InviteDetails(
    val token: String,
    val teamName: String,
    val clubName: String,
    val role: String,
    val invitedBy: String,
    val expiresAt: String,
    val alreadyRedeemed: Boolean
)
