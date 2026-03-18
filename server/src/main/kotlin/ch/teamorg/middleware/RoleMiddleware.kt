package ch.teamorg.middleware

import ch.teamorg.domain.repositories.ClubRepository
import ch.teamorg.domain.repositories.TeamRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

suspend fun ApplicationCall.requireClubRole(clubId: UUID, role: String, clubRepository: ClubRepository): Boolean {
    val principal = principal<JWTPrincipal>()
    val userId = principal?.payload?.subject?.let { UUID.fromString(it) } ?: return false

    val hasRole = clubRepository.hasRole(userId, clubId, role)
    if (!hasRole) {
        respond(HttpStatusCode.Forbidden, "You do not have the required role for this club")
        return false
    }
    return true
}

suspend fun ApplicationCall.requireTeamRole(teamId: UUID, vararg roles: String, teamRepository: TeamRepository): Boolean {
    val principal = principal<JWTPrincipal>()
    val userId = principal?.payload?.subject?.let { UUID.fromString(it) } ?: return false

    val hasRole = teamRepository.hasRole(userId, teamId, *roles)
    if (!hasRole) {
        respond(HttpStatusCode.Forbidden, "You do not have the required role for this team")
        return false
    }
    return true
}
