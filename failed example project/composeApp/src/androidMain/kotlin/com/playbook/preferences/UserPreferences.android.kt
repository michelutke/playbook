package com.playbook.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

actual class UserPreferences(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val clubIdKey = stringPreferencesKey("club_id")

    actual suspend fun getToken(): String? =
        context.dataStore.data.map { it[tokenKey] }.firstOrNull()

    actual suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    actual suspend fun clearToken() {
        context.dataStore.edit { it.remove(tokenKey) }
    }

    actual suspend fun getClubId(): String? =
        context.dataStore.data.map { it[clubIdKey] }.firstOrNull()

    actual suspend fun saveClubId(clubId: String) {
        context.dataStore.edit { it[clubIdKey] = clubId }
    }

    actual suspend fun clearClubId() {
        context.dataStore.edit { it.remove(clubIdKey) }
    }
}
