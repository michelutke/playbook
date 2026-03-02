package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.Club
import com.playbook.domain.CreateClubRequest
import com.playbook.domain.UpdateClubRequest
import com.playbook.repository.ClubRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class ClubRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : ClubRepository {

    override suspend fun create(request: CreateClubRequest, createdByUserId: String): Club =
        client.post("${config.baseUrl}/clubs") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getById(clubId: String): Club? =
        client.get("${config.baseUrl}/clubs/$clubId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun update(clubId: String, request: UpdateClubRequest): Club =
        client.patch("${config.baseUrl}/clubs/$clubId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun updateLogoUrl(clubId: String, logoUrl: String): Club =
        getById(clubId) ?: error("Club $clubId not found after logo upload")

    override suspend fun uploadLogo(clubId: String, contentType: String, imageBytes: ByteArray): Club =
        client.post("${config.baseUrl}/clubs/$clubId/logo") {
            bearerAuth(config.authTokenProvider() ?: "")
            setBody(MultiPartFormDataContent(
                formData {
                    append("logo", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"logo\"")
                    })
                }
            ))
        }.body()
}
