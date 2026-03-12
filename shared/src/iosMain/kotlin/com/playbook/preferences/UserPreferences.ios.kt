package com.playbook.preferences

import com.russhwolf.multiplatform.settings.NSUserDefaultsSettings
import com.russhwolf.multiplatform.settings.Settings
import platform.Foundation.NSUserDefaults

actual class UserPreferences {
    private val settings: Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

    actual fun saveToken(token: String) {
        settings.putString("auth_token", token)
    }

    actual fun getToken(): String? {
        return settings.getStringOrNull("auth_token")
    }

    actual fun clearToken() {
        settings.remove("auth_token")
        settings.remove("user_id")
    }

    actual fun saveUserId(id: String) {
        settings.putString("user_id", id)
    }

    actual fun getUserId(): String? {
        return settings.getStringOrNull("user_id")
    }
}
