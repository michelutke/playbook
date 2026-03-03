package com.playbook.push

interface PushService {
    suspend fun send(
        userIds: List<String>,
        title: String,
        body: String,
        deepLink: String,
        data: Map<String, String> = emptyMap(),
    )
}
