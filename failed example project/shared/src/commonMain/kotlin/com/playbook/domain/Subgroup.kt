package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Subgroup(
    val id: String,
    val teamId: String,
    val name: String,
    val memberCount: Int = 0,
    val memberIds: List<String> = emptyList(),
    val createdAt: Instant,
)

@Serializable
data class CreateSubgroupRequest(
    val name: String,
    val memberIds: List<String> = emptyList(),
)

@Serializable
data class UpdateSubgroupRequest(
    val name: String? = null,
    val memberIds: List<String>? = null,
)
