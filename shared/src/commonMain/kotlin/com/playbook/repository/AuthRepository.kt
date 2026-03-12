package com.playbook.repository

import com.playbook.domain.AuthResponse
import com.playbook.domain.AuthUser
import com.playbook.domain.LoginRequest
import com.playbook.domain.RegisterRequest

interface AuthRepository {
    suspend fun register(request: RegisterRequest): Result<AuthResponse>
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    fun logout()
    fun isLoggedIn(): Boolean
    suspend fun getMe(): Result<AuthUser>
}
