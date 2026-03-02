package com.playbook.repository

import com.playbook.domain.Club
import com.playbook.domain.CreateClubRequest
import com.playbook.domain.UpdateClubRequest

interface ClubRepository {
    suspend fun create(request: CreateClubRequest, createdByUserId: String): Club
    suspend fun getById(clubId: String): Club?
    suspend fun update(clubId: String, request: UpdateClubRequest): Club
    suspend fun updateLogoUrl(clubId: String, logoUrl: String): Club
    suspend fun uploadLogo(clubId: String, contentType: String, imageBytes: ByteArray): Club
}
