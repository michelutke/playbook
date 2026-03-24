package ch.teamorg.domain

import kotlinx.serialization.Serializable

@Serializable
data class AbwesenheitRule(
    val id: String,
    val userId: String,
    val presetType: String,    // "holidays"|"injury"|"work"|"school"|"travel"|"other"
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,      // "recurring"|"period"
    val weekdays: List<Int>? = null,  // 0=Mon..6=Sun
    val startDate: String? = null,    // ISO date "2026-04-01"
    val endDate: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAbwesenheitRequest(
    val presetType: String,
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
data class UpdateAbwesenheitRequest(
    val presetType: String? = null,
    val label: String? = null,
    val bodyPart: String? = null,
    val ruleType: String? = null,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null
)
