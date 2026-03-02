package com.playbook.repository

import com.playbook.domain.CreateInviteRequest
import com.playbook.domain.Invite
import com.playbook.domain.InviteContext
import com.playbook.domain.InviteStatus

interface InviteRepository {
    suspend fun create(teamId: String, request: CreateInviteRequest, invitedByUserId: String): Invite
    suspend fun listPending(teamId: String): List<Invite>
    suspend fun revoke(inviteId: String)
    suspend fun resolveToken(token: String): InviteContext?
    suspend fun accept(token: String, userId: String)
}
