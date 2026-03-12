package com.playbook.repository

import com.playbook.domain.*
import kotlinx.coroutines.flow.Flow

interface AbwesenheitRepository {
    suspend fun listRules(userId: String): List<AbwesenheitRule>
    suspend fun createRule(userId: String, request: CreateAbwesenheitRuleRequest): CreateAbwesenheitResponse
    suspend fun updateRule(ruleId: String, userId: String, request: UpdateAbwesenheitRuleRequest): AbwesenheitRule
    suspend fun deleteRule(ruleId: String, userId: String)
    suspend fun getBackfillStatus(userId: String): BackfillJobStatus
    fun pollBackfillStatus(intervalMs: Long = 2000L): Flow<BackfillJobStatus>
}
