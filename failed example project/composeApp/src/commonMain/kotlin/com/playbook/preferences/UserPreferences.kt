package com.playbook.preferences

expect class UserPreferences {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    suspend fun getClubId(): String?
    suspend fun saveClubId(clubId: String)
    suspend fun clearClubId()
}
