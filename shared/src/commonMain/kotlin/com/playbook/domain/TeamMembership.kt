package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class MemberRole { COACH, PLAYER }

@Serializable
data class TeamMembership(
    val id: String,
    val teamId: String,
    val userId: String,
    val role: MemberRole,
    val addedBy: String?,
    val joinedAt: Instant,
)

@Serializable
data class RosterMember(
    val userId: String,
    val displayName: String?,
    val avatarUrl: String?,
    val roles: List<MemberRole>,
    val joinedAt: Instant,
)

@Serializable
data class AddRoleRequest(
    val role: MemberRole,
)
