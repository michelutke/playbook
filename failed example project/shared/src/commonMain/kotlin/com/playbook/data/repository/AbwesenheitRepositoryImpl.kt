package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.*
import com.playbook.repository.AbwesenheitRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AbwesenheitRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : AbwesenheitRepository {

    override suspend fun listRules(userId: String): List<AbwesenheitRule> =
        client.get("${config.baseUrl}/users/me/abwesenheit") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun createRule(userId: String, request: CreateAbwesenheitRuleRequest): CreateAbwesenheitResponse =
        client.post("${config.baseUrl}/users/me/abwesenheit") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun updateRule(ruleId: String, userId: String, request: UpdateAbwesenheitRuleRequest): AbwesenheitRule =
        client.put("${config.baseUrl}/users/me/abwesenheit/$ruleId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun deleteRule(ruleId: String, userId: String) {
        client.delete("${config.baseUrl}/users/me/abwesenheit/$ruleId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun getBackfillStatus(userId: String): BackfillJobStatus =
        client.get("${config.baseUrl}/users/me/abwesenheit/backfill-status") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override fun pollBackfillStatus(intervalMs: Long): Flow<BackfillJobStatus> = flow {
        while (true) {
            val status = getBackfillStatus(userId = "")
            emit(status)
            if (status.status == "done" || status.status == "failed") break
            delay(intervalMs)
        }
    }
}
