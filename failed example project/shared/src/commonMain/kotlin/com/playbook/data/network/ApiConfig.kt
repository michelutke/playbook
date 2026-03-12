package com.playbook.data.network

data class ApiConfig(
    val baseUrl: String,
    val authTokenProvider: () -> String?,
    val userIdProvider: (() -> String?)? = null,
)
