package ch.teamorg.repository

import ch.teamorg.domain.AbwesenheitRule
import ch.teamorg.domain.BackfillStatus
import ch.teamorg.domain.CreateAbwesenheitRequest
import ch.teamorg.domain.UpdateAbwesenheitRequest

interface AbwesenheitRepository {
    suspend fun listRules(): Result<List<AbwesenheitRule>>
    suspend fun createRule(request: CreateAbwesenheitRequest): Result<AbwesenheitRule>
    suspend fun updateRule(ruleId: String, request: UpdateAbwesenheitRequest): Result<AbwesenheitRule>
    suspend fun deleteRule(ruleId: String): Result<Unit>
    suspend fun getBackfillStatus(): Result<BackfillStatus>
}
