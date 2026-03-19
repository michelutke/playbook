package ch.teamorg.routes

import ch.teamorg.db.tables.SubGroupMembersTable
import ch.teamorg.db.tables.SubGroupsTable
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

fun Route.subGroupRoutes() {
    authenticate("jwt") {
        get("/teams/{teamId}/subgroups") {
            val teamId = UUID.fromString(call.parameters["teamId"])
            val subGroups = newSuspendedTransaction {
                SubGroupsTable
                    .leftJoin(SubGroupMembersTable)
                    .selectAll()
                    .where { SubGroupsTable.teamId eq teamId }
                    .groupBy { it[SubGroupsTable.id] }
                    .map { (groupId, rows) ->
                        val first = rows.first()
                        mapOf(
                            "id" to groupId.toString(),
                            "teamId" to first[SubGroupsTable.teamId].toString(),
                            "name" to first[SubGroupsTable.name],
                            "createdAt" to first[SubGroupsTable.createdAt].toString(),
                            "memberCount" to rows.count { it.getOrNull(SubGroupMembersTable.userId) != null }.toLong()
                        )
                    }
            }
            call.respond(subGroups)
        }
    }
}
