package ch.teamorg.domain.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val isSuperAdmin: Boolean
)
