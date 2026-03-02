package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class InviteType { TEAM_PLAYER, TEAM_COACH }
enum class InviteStatus { PENDING, ACCEPTED, EXPIRED, REVOKED }

@Serializable
data class Invite(
    val id: String,
    val inviteType: InviteType,
    val teamId: String,
    val role: MemberRole,
    val invitedEmail: String,
    val inviteToken: String,
    val status: InviteStatus,
    val invitedBy: String?,
    val expiresAt: Instant,
    val createdAt: Instant,
    val acceptedAt: Instant?,
)

@Serializable
data class CreateInviteRequest(
    val email: String,
    val role: MemberRole,
)

@Serializable
data class InviteContext(
    val invite: Invite,
    val teamName: String,
    val clubName: String,
)
