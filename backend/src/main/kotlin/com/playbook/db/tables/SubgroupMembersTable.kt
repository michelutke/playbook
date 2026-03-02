package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object SubgroupMembersTable : Table("subgroup_members") {
    val subgroupId = uuid("subgroup_id").references(SubgroupsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(subgroupId, userId)
}
