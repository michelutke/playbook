package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.*
import com.playbook.repository.CoachLinkRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CoachLinkRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : CoachLinkRepository {

    override suspend fun getActive(clubId: String): CoachLink? =
        client.get("${config.baseUrl}/clubs/$clubId/coach-link") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun rotate(clubId: String, request: CreateCoachLinkRequest, createdByUserId: String): CoachLink =
        client.post("${config.baseUrl}/clubs/$clubId/coach-link") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun revoke(clubId: String) {
        client.delete("${config.baseUrl}/clubs/$clubId/coach-link") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun resolveToken(token: String): CoachLinkContext? =
        client.get("${config.baseUrl}/club-links/$token").body()

    override suspend fun join(token: String, userId: String) {
        client.post("${config.baseUrl}/club-links/$token/join") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }
}
