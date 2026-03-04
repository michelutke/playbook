package com.playbook.preferences

import platform.Foundation.NSUserDefaults

actual class UserPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun getToken(): String? = defaults.stringForKey("auth_token")

    actual suspend fun saveToken(token: String) {
        defaults.setObject(token, "auth_token")
    }

    actual suspend fun clearToken() {
        defaults.removeObjectForKey("auth_token")
    }

    actual suspend fun getClubId(): String? = defaults.stringForKey("club_id")

    actual suspend fun saveClubId(clubId: String) {
        defaults.setObject(clubId, "club_id")
    }

    actual suspend fun clearClubId() {
        defaults.removeObjectForKey("club_id")
    }
}
