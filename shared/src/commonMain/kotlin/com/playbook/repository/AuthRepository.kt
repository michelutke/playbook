package com.playbook.repository

import com.playbook.domain.AuthResponse
import com.playbook.domain.LoginRequest
import com.playbook.domain.RegisterRequest
import com.playbook.domain.UserProfile

interface AuthRepository {
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun login(request: LoginRequest): AuthResponse
    suspend fun getMe(token: String): UserProfile
}
