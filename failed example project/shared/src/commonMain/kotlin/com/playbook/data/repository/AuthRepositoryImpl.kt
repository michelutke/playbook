package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.AuthResponse
import com.playbook.domain.LoginRequest
import com.playbook.domain.RegisterRequest
import com.playbook.domain.UserProfile
import com.playbook.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : AuthRepository {

    override suspend fun register(request: RegisterRequest): AuthResponse =
        client.post("${config.baseUrl}/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun login(request: LoginRequest): AuthResponse =
        client.post("${config.baseUrl}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getMe(token: String): UserProfile =
        client.get("${config.baseUrl}/users/me") {
            bearerAuth(token)
        }.body()
}
