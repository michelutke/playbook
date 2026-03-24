package ch.teamorg.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class InviteLink(
    val id: String,
    val token: String,
    val teamId: String,
    val invitedByUserId: String,
    val invitedEmail: String?,
    val role: String,
    val expiresAt: String,
    val redeemedAt: String?,
    val redeemedByUserId: String?,
    val createdAt: String
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
