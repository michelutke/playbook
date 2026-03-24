package ch.teamorg.domain.repositories

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AbwesenheitRuleRow(
    val id: UUID,
    val userId: UUID,
    val presetType: String,
    val label: String,
    val bodyPart: String?,
    val ruleType: String,
    val weekdays: List<Short>?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateAbwesenheitRule(
    val presetType: String,
    val label: String,
    val bodyPart: String?,
    val ruleType: String,
    val weekdays: List<Short>?,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

data class UpdateAbwesenheitRule(
    val presetType: String?,
    val label: String?,
    val bodyPart: String?,
    val ruleType: String?,
    val weekdays: List<Short>?,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

interface AbwesenheitRepository {
    suspend fun listRules(userId: UUID): List<AbwesenheitRuleRow>
    suspend fun createRule(userId: UUID, rule: CreateAbwesenheitRule): AbwesenheitRuleRow
    suspend fun updateRule(ruleId: UUID, rule: UpdateAbwesenheitRule): AbwesenheitRuleRow
    suspend fun deleteRule(ruleId: UUID)
    suspend fun getRule(ruleId: UUID): AbwesenheitRuleRow?
}
