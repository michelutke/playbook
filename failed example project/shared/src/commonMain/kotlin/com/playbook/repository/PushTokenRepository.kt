package com.playbook.repository

interface PushTokenRepository {
    suspend fun registerToken(platform: String, token: String)
    suspend fun deregisterToken(token: String)
}
