package com.playbook.domain

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String)

@Serializable
data class UserProfile(val id: String, val email: String, val displayName: String?, val clubId: String?)
