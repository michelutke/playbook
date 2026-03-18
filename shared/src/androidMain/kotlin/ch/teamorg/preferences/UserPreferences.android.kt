package ch.teamorg.preferences

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class UserPreferences(context: Context) {
    private val settings: Settings = SharedPreferencesSettings(
        context.getSharedPreferences("teamorg_prefs", Context.MODE_PRIVATE)
    )

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
