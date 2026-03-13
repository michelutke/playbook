package com.playbook.preferences

import com.russhwolf.settings.Settings

expect class UserPreferences {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    fun saveUserId(id: String)
    fun getUserId(): String?
}
