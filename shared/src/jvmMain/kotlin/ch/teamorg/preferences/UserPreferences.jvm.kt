package ch.teamorg.preferences

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual class UserPreferences {
    private val settings: Settings = PreferencesSettings(Preferences.userRoot().node("ch.teamorg"))

    actual fun saveToken(token: String) {
        settings.putString("auth_token", token)
    }

    actual fun getToken(): String? = settings.getStringOrNull("auth_token")

    actual fun clearToken() {
        settings.remove("auth_token")
        settings.remove("user_id")
    }

    actual fun saveUserId(id: String) {
        settings.putString("user_id", id)
    }

    actual fun getUserId(): String? = settings.getStringOrNull("user_id")
}
