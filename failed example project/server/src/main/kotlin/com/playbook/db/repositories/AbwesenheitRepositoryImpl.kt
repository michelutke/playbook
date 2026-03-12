package com.playbook.db.repositories

import com.playbook.db.tables.AbwesenheitBackfillJobsTable
import com.playbook.db.tables.AbwesenheitRulesTable
import com.playbook.domain.AbwesenheitPresetType
import com.playbook.domain.AbwesenheitRule
import com.playbook.domain.AbwesenheitRuleType
import com.playbook.domain.BackfillJobStatus
import com.playbook.domain.CreateAbwesenheitResponse
import com.playbook.domain.CreateAbwesenheitRuleRequest
import com.playbook.domain.UpdateAbwesenheitRuleRequest
import com.playbook.plugins.ForbiddenException
import com.playbook.plugins.NotFoundException
import com.playbook.repository.AbwesenheitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AbwesenheitRepositoryImpl : AbwesenheitRepository {

    override suspend fun listRules(userId: String): List<AbwesenheitRule> = newSuspendedTransaction {
        val uid = UUID.fromString(userId)
        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.userId eq uid }
            .map { it.toAbwesenheitRule() }
    }

    override suspend fun createRule(userId: String, request: CreateAbwesenheitRuleRequest): CreateAbwesenheitResponse =
        newSuspendedTransaction {
            val uid = UUID.fromString(userId)
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val ruleId = UUID.randomUUID()

            AbwesenheitRulesTable.insert {
                it[id] = ruleId
                it[AbwesenheitRulesTable.userId] = uid
                it[presetType] = request.presetType.name.lowercase()
                it[label] = request.label
                it[ruleType] = request.ruleType.name.lowercase()
                it[weekdays] = request.weekdays?.joinToString(",")
                it[startDate] = request.startDate
                it[endDate] = request.endDate
                it[createdAt] = now
                it[updatedAt] = now
            }

            val jobId = UUID.randomUUID()
            AbwesenheitBackfillJobsTable.insert {
                it[id] = jobId
                it[AbwesenheitBackfillJobsTable.ruleId] = ruleId
                it[status] = "pending"
                it[createdAt] = now
                it[updatedAt] = now
            }

            val rule = AbwesenheitRulesTable.selectAll()
                .where { AbwesenheitRulesTable.id eq ruleId }
                .single()
                .toAbwesenheitRule()

            CreateAbwesenheitResponse(rule = rule, jobId = jobId.toString())
        }

    override suspend fun updateRule(
        ruleId: String,
        userId: String,
        request: UpdateAbwesenheitRuleRequest,
    ): AbwesenheitRule = newSuspendedTransaction {
        val rid = UUID.fromString(ruleId)
        val uid = UUID.fromString(userId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val existing = AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq rid }
            .singleOrNull() ?: throw NotFoundException("Rule not found")

        if (existing[AbwesenheitRulesTable.userId] != uid) throw ForbiddenException("Not your rule")

        AbwesenheitRulesTable.update({ AbwesenheitRulesTable.id eq rid }) {
            request.presetType?.let { v -> it[presetType] = v.name.lowercase() }
            request.label?.let { v -> it[label] = v }
            request.ruleType?.let { v -> it[ruleType] = v.name.lowercase() }
            request.weekdays?.let { v -> it[weekdays] = v.joinToString(",") }
            request.startDate?.let { v -> it[startDate] = v }
            request.endDate?.let { v -> it[endDate] = v }
            it[updatedAt] = now
        }

        // Queue new backfill job for updated rule
        AbwesenheitBackfillJobsTable.insert {
            it[id] = UUID.randomUUID()
            it[AbwesenheitBackfillJobsTable.ruleId] = rid
            it[status] = "pending"
            it[createdAt] = now
            it[updatedAt] = now
        }

        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq rid }
            .single()
            .toAbwesenheitRule()
    }

    override suspend fun deleteRule(ruleId: String, userId: String) = newSuspendedTransaction {
        val rid = UUID.fromString(ruleId)
        val uid = UUID.fromString(userId)

        val existing = AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq rid }
            .singleOrNull() ?: throw NotFoundException("Rule not found")

        if (existing[AbwesenheitRulesTable.userId] != uid) throw ForbiddenException("Not your rule")

        AbwesenheitRulesTable.deleteWhere { id eq rid }
        Unit
    }

    override suspend fun getBackfillStatus(userId: String): BackfillJobStatus = newSuspendedTransaction {
        val uid = UUID.fromString(userId)

        val ruleIds = AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.userId eq uid }
            .map { it[AbwesenheitRulesTable.id] }

        if (ruleIds.isEmpty()) return@newSuspendedTransaction BackfillJobStatus(status = "done")

        val latestJob = AbwesenheitBackfillJobsTable.selectAll()
            .where { AbwesenheitBackfillJobsTable.ruleId inList ruleIds }
            .orderBy(AbwesenheitBackfillJobsTable.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull() ?: throw NotFoundException("No backfill jobs found")

        BackfillJobStatus(status = latestJob[AbwesenheitBackfillJobsTable.status])
    }

    private fun ResultRow.toAbwesenheitRule() = AbwesenheitRule(
        id = this[AbwesenheitRulesTable.id].toString(),
        userId = this[AbwesenheitRulesTable.userId].toString(),
        presetType = this[AbwesenheitRulesTable.presetType].toPresetType(),
        label = this[AbwesenheitRulesTable.label],
        ruleType = this[AbwesenheitRulesTable.ruleType].toRuleType(),
        weekdays = this[AbwesenheitRulesTable.weekdays]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() },
        startDate = this[AbwesenheitRulesTable.startDate],
        endDate = this[AbwesenheitRulesTable.endDate],
        createdAt = this[AbwesenheitRulesTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[AbwesenheitRulesTable.updatedAt].toInstant().toKotlinInstant(),
    )

    private fun String.toPresetType() = when (this.uppercase()) {
        "HOLIDAYS" -> AbwesenheitPresetType.HOLIDAYS
        "INJURY" -> AbwesenheitPresetType.INJURY
        "WORK" -> AbwesenheitPresetType.WORK
        "SCHOOL" -> AbwesenheitPresetType.SCHOOL
        "TRAVEL" -> AbwesenheitPresetType.TRAVEL
        else -> AbwesenheitPresetType.OTHER
    }

    private fun String.toRuleType() = when (this.uppercase()) {
        "RECURRING" -> AbwesenheitRuleType.RECURRING
        else -> AbwesenheitRuleType.PERIOD
    }

    override fun pollBackfillStatus(intervalMs: Long): Flow<BackfillJobStatus> =
        throw UnsupportedOperationException("pollBackfillStatus is not supported on the server")
}
