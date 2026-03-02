package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CoachLink(
    val id: String,
    val clubId: String,
    val token: String,
    val expiresAt: Instant,
    val createdBy: String?,
    val createdAt: Instant,
    val revokedAt: Instant?,
)

val CoachLink.isActive: Boolean get() = revokedAt == null

@Serializable
data class CreateCoachLinkRequest(
    val expiresInDays: Int = 7,
)

@Serializable
data class CoachLinkContext(
    val coachLink: CoachLink,
    val clubName: String,
    val clubSportType: String,
)
