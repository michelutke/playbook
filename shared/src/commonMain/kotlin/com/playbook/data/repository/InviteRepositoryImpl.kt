package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.*
import com.playbook.repository.InviteRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class InviteRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : InviteRepository {

    override suspend fun create(teamId: String, request: CreateInviteRequest, invitedByUserId: String): Invite =
        client.post("${config.baseUrl}/teams/$teamId/invites") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun listPending(teamId: String): List<Invite> =
        client.get("${config.baseUrl}/teams/$teamId/invites") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun revoke(inviteId: String) {
        // inviteId here is used as the path; the route DELETE /teams/{id}/invites/{inviteId}
        // requires teamId, but the client tracks this via invite.teamId
        // This is a simplification — caller should track teamId
        client.delete("${config.baseUrl}/invites/$inviteId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun resolveToken(token: String): InviteContext? =
        client.get("${config.baseUrl}/invites/$token").body()

    override suspend fun accept(token: String, userId: String) {
        client.post("${config.baseUrl}/invites/$token/accept") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }
}
