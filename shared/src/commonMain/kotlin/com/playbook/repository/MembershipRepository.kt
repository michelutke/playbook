package com.playbook.repository

import com.playbook.domain.MemberRole
import com.playbook.domain.PlayerProfile
import com.playbook.domain.RosterMember
import com.playbook.domain.UpdatePlayerProfileRequest

interface MembershipRepository {
    suspend fun getRoster(teamId: String): List<RosterMember>
    suspend fun addRole(teamId: String, userId: String, role: MemberRole, addedByUserId: String)
    suspend fun removeRole(teamId: String, userId: String, role: MemberRole)
    suspend fun removeMember(teamId: String, userId: String)
    suspend fun leaveTeam(teamId: String, userId: String)
    suspend fun getProfile(teamId: String, userId: String): PlayerProfile?
    suspend fun updateProfile(teamId: String, userId: String, request: UpdatePlayerProfileRequest): PlayerProfile
    suspend fun hasRole(teamId: String, userId: String, role: MemberRole): Boolean
}
