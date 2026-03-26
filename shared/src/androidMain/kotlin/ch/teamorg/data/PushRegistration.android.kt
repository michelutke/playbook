package ch.teamorg.data

import com.onesignal.OneSignal

actual object PushRegistration {
    actual fun login(userId: String) {
        OneSignal.login(userId)
    }

    actual fun logout() {
        OneSignal.logout()
    }
}
