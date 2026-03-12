package com.playbook.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun initPushSdk() {
    // Called by iosApp Swift side via OneSignalWrapper
}

actual fun requestPushPermission(): Flow<PermissionState> = flow {
    emit(PermissionState.NOT_DETERMINED)
}

actual fun handleDeepLink(link: String) {
    // iOS navigation handled via Swift side
}

actual fun loginPushUser(userId: String) {
    // Delegated to Swift side
}

actual fun logoutPushUser() {
    // Delegated to Swift side
}
