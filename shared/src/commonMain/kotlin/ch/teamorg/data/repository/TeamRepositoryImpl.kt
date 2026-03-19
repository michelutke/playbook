package ch.teamorg.data.repository

import ch.teamorg.domain.TeamMember
import ch.teamorg.domain.UserRoles
import ch.teamorg.repository.TeamRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class CreateInviteRequest(val role: String, val email: String?)

@Serializable
private data class InviteResponse(val token: String, val inviteUrl: String, val expiresAt: String)

@Serializable
private data class UpdateRoleRequest(val role: String)

class TeamRepositoryImpl(private val client: HttpClient) : TeamRepository {
    override suspend fun getTeamRoster(teamId: String): Result<List<TeamMember>> {
        return try {
            val response = client.get("/teams/$teamId/members")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch roster: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(teamId: String, userId: String): Result<Unit> {
        return try {
            val response = client.delete("/teams/$teamId/members/$userId")
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove member: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createInvite(teamId: String, role: String, email: String?): Result<String> {
        return try {
            val response = client.post("/teams/$teamId/invites") {
                setBody(CreateInviteRequest(role, email))
            }
            if (response.status == HttpStatusCode.Created) {
                val inviteResponse = response.body<InviteResponse>()
                Result.success(inviteResponse.inviteUrl)
            } else {
                Result.failure(Exception("Failed to create invite: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyRoles(): Result<UserRoles> {
        return try {
            val response = client.get("/auth/me/roles")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch roles: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberRole(teamId: String, userId: String, role: String): Result<TeamMember> {
        return try {
            val response = client.patch("/teams/$teamId/members/$userId/role") {
                contentType(ContentType.Application.Json)
                setBody(UpdateRoleRequest(role))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to update role: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
