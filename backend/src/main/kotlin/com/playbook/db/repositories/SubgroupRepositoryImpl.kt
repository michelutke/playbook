package com.playbook.db.repositories

import com.playbook.db.tables.SubgroupMembersTable
import com.playbook.db.tables.SubgroupsTable
import com.playbook.domain.CreateSubgroupRequest
import com.playbook.domain.Subgroup
import com.playbook.domain.UpdateSubgroupRequest
import com.playbook.repository.SubgroupRepository
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SubgroupRepositoryImpl : SubgroupRepository {

    override suspend fun listForTeam(teamId: String): List<Subgroup> =
        newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            val subgroupRows = SubgroupsTable.select { SubgroupsTable.teamId eq tid }.toList()
            val subgroupIds = subgroupRows.map { it[SubgroupsTable.id] }

            val memberRows = if (subgroupIds.isEmpty()) emptyList()
            else SubgroupMembersTable.select { SubgroupMembersTable.subgroupId inList subgroupIds }.toList()

            val membersBySubgroup = memberRows.groupBy { it[SubgroupMembersTable.subgroupId] }
            subgroupRows.map { row ->
                val members = membersBySubgroup[row[SubgroupsTable.id]] ?: emptyList()
                row.toSubgroup(members.map { it[SubgroupMembersTable.userId].toString() })
            }
        }

    override suspend fun create(teamId: String, request: CreateSubgroupRequest): Subgroup =
        newSuspendedTransaction {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            SubgroupsTable.insert {
                it[SubgroupsTable.id] = id
                it[SubgroupsTable.teamId] = UUID.fromString(teamId)
                it[name] = request.name
                it[createdAt] = now
            }
            insertMembers(id, request.memberIds)
            fetchSubgroup(id)
        }

    override suspend fun update(subgroupId: String, request: UpdateSubgroupRequest): Subgroup =
        newSuspendedTransaction {
            val id = UUID.fromString(subgroupId)
            request.name?.let { newName ->
                SubgroupsTable.update({ SubgroupsTable.id eq id }) { it[name] = newName }
            }
            request.memberIds?.let { memberIds ->
                SubgroupMembersTable.deleteWhere { SubgroupMembersTable.subgroupId eq id }
                insertMembers(id, memberIds)
            }
            fetchSubgroup(id)
        }

    override suspend fun delete(subgroupId: String) =
        newSuspendedTransaction {
            SubgroupsTable.deleteWhere { id eq UUID.fromString(subgroupId) }
        }.let {}

    private fun insertMembers(subgroupId: UUID, memberIds: List<String>) {
        memberIds.forEach { userId ->
            SubgroupMembersTable.insertIgnore {
                it[SubgroupMembersTable.subgroupId] = subgroupId
                it[SubgroupMembersTable.userId] = UUID.fromString(userId)
            }
        }
    }

    private fun fetchSubgroup(id: UUID): Subgroup {
        val row = SubgroupsTable.select { SubgroupsTable.id eq id }.single()
        val members = SubgroupMembersTable.select { SubgroupMembersTable.subgroupId eq id }
            .map { it[SubgroupMembersTable.userId].toString() }
        return row.toSubgroup(members)
    }

    private fun ResultRow.toSubgroup(memberIds: List<String>) = Subgroup(
        id = this[SubgroupsTable.id].toString(),
        teamId = this[SubgroupsTable.teamId].toString(),
        name = this[SubgroupsTable.name],
        memberCount = memberIds.size,
        memberIds = memberIds,
        createdAt = this[SubgroupsTable.createdAt].toInstant().toKotlinInstant(),
    )
}
