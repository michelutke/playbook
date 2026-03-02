package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class ClubStatus { ACTIVE, INACTIVE }

@Serializable
data class Club(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val sportType: String,
    val location: String?,
    val status: ClubStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateClubRequest(
    val name: String,
    val sportType: String,
    val location: String? = null,
)

@Serializable
data class UpdateClubRequest(
    val name: String? = null,
    val sportType: String? = null,
    val location: String? = null,
)
