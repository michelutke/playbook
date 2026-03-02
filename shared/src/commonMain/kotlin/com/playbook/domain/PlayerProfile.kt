package com.playbook.domain

import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val teamId: String,
    val userId: String,
    val jerseyNumber: Int?,
    val position: String?,
)

@Serializable
data class UpdatePlayerProfileRequest(
    val jerseyNumber: Int? = null,
    val position: String? = null,
)
