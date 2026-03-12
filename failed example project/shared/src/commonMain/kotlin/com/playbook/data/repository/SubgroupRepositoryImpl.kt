package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.CreateSubgroupRequest
import com.playbook.domain.Subgroup
import com.playbook.domain.UpdateSubgroupRequest
import com.playbook.repository.SubgroupRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SubgroupRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : SubgroupRepository {

    override suspend fun listForTeam(teamId: String): List<Subgroup> =
        client.get("${config.baseUrl}/teams/$teamId/subgroups") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun create(teamId: String, request: CreateSubgroupRequest): Subgroup =
        client.post("${config.baseUrl}/teams/$teamId/subgroups") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(subgroupId: String, request: UpdateSubgroupRequest): Subgroup =
        client.patch("${config.baseUrl}/subgroups/$subgroupId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun delete(subgroupId: String) {
        client.delete("${config.baseUrl}/subgroups/$subgroupId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }
}
