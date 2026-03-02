package com.playbook.repository

import com.playbook.domain.CoachLink
import com.playbook.domain.CoachLinkContext
import com.playbook.domain.CreateCoachLinkRequest

interface CoachLinkRepository {
    suspend fun getActive(clubId: String): CoachLink?
    suspend fun rotate(clubId: String, request: CreateCoachLinkRequest, createdByUserId: String): CoachLink
    suspend fun revoke(clubId: String)
    suspend fun resolveToken(token: String): CoachLinkContext?
    suspend fun join(token: String, userId: String)
}
