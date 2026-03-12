package com.playbook.repository

import com.playbook.domain.CreateSubgroupRequest
import com.playbook.domain.Subgroup
import com.playbook.domain.UpdateSubgroupRequest

interface SubgroupRepository {
    suspend fun listForTeam(teamId: String): List<Subgroup>
    suspend fun create(teamId: String, request: CreateSubgroupRequest): Subgroup
    suspend fun update(subgroupId: String, request: UpdateSubgroupRequest): Subgroup
    suspend fun delete(subgroupId: String)
}
