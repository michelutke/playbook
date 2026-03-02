package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.*
import com.playbook.repository.MembershipRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class MembershipRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : MembershipRepository {

    override suspend fun getRoster(teamId: String): List<RosterMember> =
        client.get("${config.baseUrl}/teams/$teamId/members") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun addRole(teamId: String, userId: String, role: MemberRole, addedByUserId: String) {
        client.post("${config.baseUrl}/teams/$teamId/members/$userId/roles") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(AddRoleRequest(role))
        }
    }

    override suspend fun removeRole(teamId: String, userId: String, role: MemberRole) {
        client.delete("${config.baseUrl}/teams/$teamId/members/$userId/roles/${role.name.lowercase()}") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun removeMember(teamId: String, userId: String) {
        client.delete("${config.baseUrl}/teams/$teamId/members/$userId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun leaveTeam(teamId: String, userId: String) {
        client.delete("${config.baseUrl}/users/me/teams/$teamId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }

    override suspend fun getProfile(teamId: String, userId: String): PlayerProfile? =
        client.get("${config.baseUrl}/teams/$teamId/members/$userId/profile") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun updateProfile(teamId: String, userId: String, request: UpdatePlayerProfileRequest): PlayerProfile =
        client.patch("${config.baseUrl}/teams/$teamId/members/$userId/profile") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun hasRole(teamId: String, userId: String, role: MemberRole): Boolean {
        val roster = getRoster(teamId)
        return roster.any { it.userId == userId && role in it.roles }
    }
}
