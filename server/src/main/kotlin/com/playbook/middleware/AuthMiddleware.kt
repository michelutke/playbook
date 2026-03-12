package com.playbook.middleware

import com.playbook.domain.models.User
import com.playbook.domain.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject
import java.util.*

class UserPrincipal(val userId: UUID, val isSuperAdmin: Boolean) : Principal

suspend fun PipelineContext<Unit, ApplicationCall>.authenticateUser(
    userRepository: UserRepository,
    body: suspend (User) -> Unit
) {
    val principal = call.principal<JWTPrincipal>()
    val userIdString = principal?.payload?.subject
    
    if (userIdString == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid token payload")
        return
    }

    val userId = try {
        UUID.fromString(userIdString)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid user ID format")
        return
    }

    val user = userRepository.findById(userId)
    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized, "User not found")
        return
    }

    body(user)
}
