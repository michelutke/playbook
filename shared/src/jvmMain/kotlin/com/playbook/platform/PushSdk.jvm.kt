package com.playbook.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun initPushSdk() {}
actual fun requestPushPermission(): Flow<PermissionState> = flow { emit(PermissionState.GRANTED) }
actual fun handleDeepLink(link: String) {}
actual fun loginPushUser(userId: String) {}
actual fun logoutPushUser() {}
