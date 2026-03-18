package ch.teamorg.middleware

import ch.teamorg.domain.models.User
import ch.teamorg.domain.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject
import java.util.*

class UserPrincipal(val userId: UUID, val isSuperAdmin: Boolean) : Principal

suspend fun ApplicationCall.authenticateUser(
    userRepository: UserRepository,
    body: suspend (User) -> Unit
) {
    val principal = principal<JWTPrincipal>()
    val userIdString = principal?.payload?.subject

    if (userIdString == null) {
        respond(HttpStatusCode.Unauthorized, "Invalid token payload")
        return
    }

    val userId = try {
        UUID.fromString(userIdString)
    } catch (e: Exception) {
        respond(HttpStatusCode.Unauthorized, "Invalid user ID format")
        return
    }

    val user = userRepository.findById(userId)
    if (user == null) {
        respond(HttpStatusCode.Unauthorized, "User not found")
        return
    }

    body(user)
}
