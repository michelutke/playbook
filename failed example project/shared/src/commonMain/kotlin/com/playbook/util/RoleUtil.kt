package com.playbook.util

import com.playbook.domain.MemberRole
import com.playbook.repository.MembershipRepository

/**
 * TM-050: Shared domain utility to check if a user has a specific role on a team.
 * Used across all features that gate on roles.
 */
suspend fun MembershipRepository.hasRole(teamId: String, userId: String, role: MemberRole): Boolean =
    getRoster(teamId).any { it.userId == userId && role in it.roles }

/**
 * Returns true if user holds any role on the given team.
 */
suspend fun MembershipRepository.isMemberOfTeam(teamId: String, userId: String): Boolean =
    getRoster(teamId).any { it.userId == userId }
