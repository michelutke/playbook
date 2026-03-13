package com.playbook.repository

import com.playbook.domain.InviteDetails

interface InviteRepository {
    suspend fun getInviteDetails(token: String): Result<InviteDetails>
    suspend fun redeemInvite(token: String): Result<Unit>
}
