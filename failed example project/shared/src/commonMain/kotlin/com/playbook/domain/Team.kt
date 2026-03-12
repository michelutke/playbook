package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class TeamStatus { ACTIVE, ARCHIVED, PENDING, REJECTED }

@Serializable
data class Team(
    val id: String,
    val clubId: String,
    val name: String,
    val description: String?,
    val status: TeamStatus,
    val requestedBy: String?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateTeamRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdateTeamRequest(
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class RejectTeamRequest(
    val reason: String? = null,
)
