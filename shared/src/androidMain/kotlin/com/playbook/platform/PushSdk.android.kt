package com.playbook.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun initPushSdk() {
    // OneSignal.initWithContext is called in Application.onCreate
    // This is a no-op here; wiring happens in AndroidApp
}

actual fun requestPushPermission(): Flow<PermissionState> = flow {
    // Android 13+ requires POST_NOTIFICATIONS; older = auto-granted
    // Implementation calls OneSignal.Notifications.requestPermission()
    // Placeholder: emit GRANTED for now; AndroidApp will override via OneSignal SDK
    emit(PermissionState.GRANTED)
}

actual fun handleDeepLink(link: String) {
    // Navigation handled by androidApp NavController
}

actual fun loginPushUser(userId: String) {
    // OneSignal.login(userId) — called from androidApp
}

actual fun logoutPushUser() {
    // OneSignal.logout() — called from androidApp
}
