package ch.teamorg.data

// iOS OneSignal SDK is a native Swift framework — cannot be called from Kotlin/Native.
// OneSignal.login(userId) is called from iOSApp.swift after successful auth.
actual object PushRegistration {
    actual fun login(userId: String) {
        // no-op: handled natively in iOSApp.swift
    }

    actual fun logout() {
        // no-op: handled natively in iOSApp.swift
    }
}
