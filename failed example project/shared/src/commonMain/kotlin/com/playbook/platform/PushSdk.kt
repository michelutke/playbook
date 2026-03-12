package com.playbook.platform

import kotlinx.coroutines.flow.Flow

enum class PermissionState { GRANTED, DENIED, NOT_DETERMINED }

expect fun initPushSdk()
expect fun requestPushPermission(): Flow<PermissionState>
expect fun handleDeepLink(link: String)
expect fun loginPushUser(userId: String)
expect fun logoutPushUser()
