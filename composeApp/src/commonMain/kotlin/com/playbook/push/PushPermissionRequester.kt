package com.playbook.push

expect class PushPermissionRequester {
    suspend fun requestPermission(): Boolean
}
