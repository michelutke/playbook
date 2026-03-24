package ch.teamorg.data.repository

import ch.teamorg.data.AttendanceCacheManager
import ch.teamorg.domain.AbwesenheitRule
import ch.teamorg.domain.BackfillStatus
import ch.teamorg.domain.CreateAbwesenheitRequest
import ch.teamorg.domain.UpdateAbwesenheitRequest
import ch.teamorg.preferences.UserPreferences
import ch.teamorg.repository.AbwesenheitRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException

class AbwesenheitRepositoryImpl(
    private val httpClient: HttpClient,
    private val userPreferences: UserPreferences,
    private val cacheManager: AttendanceCacheManager
) : AbwesenheitRepository {

    override suspend fun listRules(): Result<List<AbwesenheitRule>> {
        return try {
            val rules: List<AbwesenheitRule> = httpClient.get("/users/me/abwesenheit").body()
            val userId = userPreferences.getUserId() ?: ""
            cacheManager.saveRules(userId, rules)
            Result.success(rules)
        } catch (e: ConnectTimeoutException) {
            offlineFallback()
        } catch (e: HttpRequestTimeoutException) {
            offlineFallback()
        } catch (e: IOException) {
            offlineFallback()
        }
    }

    private fun offlineFallback(): Result<List<AbwesenheitRule>> {
        val userId = userPreferences.getUserId() ?: ""
        return Result.success(cacheManager.getCachedRules(userId))
    }

    override suspend fun createRule(request: CreateAbwesenheitRequest): Result<AbwesenheitRule> {
        return try {
            val rule: AbwesenheitRule = httpClient.post("/users/me/abwesenheit") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            val userId = userPreferences.getUserId() ?: ""
            val cached = cacheManager.getCachedRules(userId).toMutableList()
            cached.add(rule)
            cacheManager.saveRules(userId, cached)
            Result.success(rule)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("You're offline. Connect to create rules."))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("You're offline. Connect to create rules."))
        } catch (e: IOException) {
            Result.failure(Exception("You're offline. Connect to create rules."))
        }
    }

    override suspend fun updateRule(
        ruleId: String,
        request: UpdateAbwesenheitRequest
    ): Result<AbwesenheitRule> {
        return try {
            val rule: AbwesenheitRule = httpClient.put("/users/me/abwesenheit/$ruleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            val userId = userPreferences.getUserId() ?: ""
            val cached = cacheManager.getCachedRules(userId).map { if (it.id == ruleId) rule else it }
            cacheManager.saveRules(userId, cached)
            Result.success(rule)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("You're offline. Connect to update rules."))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("You're offline. Connect to update rules."))
        } catch (e: IOException) {
            Result.failure(Exception("You're offline. Connect to update rules."))
        }
    }

    override suspend fun deleteRule(ruleId: String): Result<Unit> {
        return try {
            httpClient.delete("/users/me/abwesenheit/$ruleId")
            cacheManager.deleteRule(ruleId)
            Result.success(Unit)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("You're offline. Connect to delete rules."))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("You're offline. Connect to delete rules."))
        } catch (e: IOException) {
            Result.failure(Exception("You're offline. Connect to delete rules."))
        }
    }

    override suspend fun getBackfillStatus(): Result<BackfillStatus> = runCatching {
        httpClient.get("/users/me/abwesenheit/backfill-status").body()
    }
}
