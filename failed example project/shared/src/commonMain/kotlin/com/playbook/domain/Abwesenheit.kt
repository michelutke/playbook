package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class AbwesenheitPresetType { HOLIDAYS, INJURY, WORK, SCHOOL, TRAVEL, OTHER }

@Serializable
enum class AbwesenheitRuleType { RECURRING, PERIOD }

@Serializable
data class AbwesenheitRule(
    val id: String,
    val userId: String,
    val presetType: AbwesenheitPresetType,
    val label: String,
    val ruleType: AbwesenheitRuleType,
    val weekdays: List<Int>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateAbwesenheitRuleRequest(
    val presetType: AbwesenheitPresetType,
    val label: String,
    val ruleType: AbwesenheitRuleType,
    val weekdays: List<Int>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

@Serializable
data class UpdateAbwesenheitRuleRequest(
    val presetType: AbwesenheitPresetType? = null,
    val label: String? = null,
    val ruleType: AbwesenheitRuleType? = null,
    val weekdays: List<Int>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

@Serializable
data class BackfillJobStatus(
    val status: String, // "pending" | "done" | "failed"
    val jobId: String? = null,
)

@Serializable
data class CreateAbwesenheitResponse(
    val rule: AbwesenheitRule,
    val jobId: String,
)
