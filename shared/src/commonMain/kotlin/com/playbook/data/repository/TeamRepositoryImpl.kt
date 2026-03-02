package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.*
import com.playbook.repository.TeamRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class TeamRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : TeamRepository {

    override suspend fun listByClub(clubId: String, statuses: List<TeamStatus>): List<Team> =
        client.get("${config.baseUrl}/clubs/$clubId/teams") {
            bearerAuth(config.authTokenProvider() ?: "")
            statuses.forEach { status -> parameter("status", status.name.lowercase()) }
        }.body()

    override suspend fun getById(teamId: String): Team? =
        client.get("${config.baseUrl}/teams/$teamId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun create(clubId: String, request: CreateTeamRequest): Team =
        client.post("${config.baseUrl}/clubs/$clubId/teams") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun submitRequest(clubId: String, request: CreateTeamRequest, requestedByUserId: String): Team =
        client.post("${config.baseUrl}/clubs/$clubId/teams/request") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(teamId: String, request: UpdateTeamRequest): Team =
        client.patch("${config.baseUrl}/teams/$teamId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun setStatus(teamId: String, status: TeamStatus): Team {
        val path = when (status) {
            TeamStatus.ARCHIVED -> "archive"
            TeamStatus.ACTIVE -> "unarchive"
            TeamStatus.PENDING -> throw IllegalArgumentException("Cannot set status to PENDING directly")
        }
        return client.post("${config.baseUrl}/teams/$teamId/$path") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()
    }

    override suspend fun approve(teamId: String): Team =
        client.post("${config.baseUrl}/teams/$teamId/approve") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun reject(teamId: String, request: RejectTeamRequest): Team =
        client.post("${config.baseUrl}/teams/$teamId/reject") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun delete(teamId: String) {
        client.delete("${config.baseUrl}/teams/$teamId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }
}
