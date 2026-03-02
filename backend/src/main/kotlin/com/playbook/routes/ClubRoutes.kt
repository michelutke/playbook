package com.playbook.routes

import com.playbook.domain.CreateClubRequest
import com.playbook.domain.UpdateClubRequest
import com.playbook.infra.DatabaseFactory
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import com.playbook.repository.ClubRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.io.File

fun Route.registerClubRoutes() {
    val clubRepo: ClubRepository by inject()

    // TM-013: POST /clubs — create club
    post("/clubs") {
        val uid = call.userId
        val request = call.receive<CreateClubRequest>()
        val club = clubRepo.create(request, uid)
        call.respond(HttpStatusCode.Created, club)
    }

    // TM-014: GET /clubs/{id}
    get("/clubs/{id}") {
        val id = call.parameters["id"]!!
        val club = clubRepo.getById(id) ?: throw NotFoundException("Club not found")
        call.respond(club)
    }

    // TM-015: PATCH /clubs/{id}
    patch("/clubs/{id}") {
        val id = call.parameters["id"]!!
        val request = call.receive<UpdateClubRequest>()
        val club = clubRepo.update(id, request)
        call.respond(club)
    }

    // TM-016: POST /clubs/{id}/logo — multipart upload
    post("/clubs/{id}/logo") {
        val id = call.parameters["id"]!!
        val multipart = call.receiveMultipart()
        var logoUrl: String? = null
        val allowedMimes = setOf("image/jpeg", "image/png", "image/webp")
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val contentType = part.contentType?.toString() ?: ""
                if (contentType !in allowedMimes) {
                    part.dispose()
                    throw IllegalArgumentException("Invalid image type. Allowed: jpeg, png, webp")
                }
                val ext = when (contentType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "bin"
                }
                val dir = File("uploads/logos").apply { mkdirs() }
                val file = File(dir, "$id.$ext")
                file.writeBytes(part.streamProvider().readBytes())
                logoUrl = "uploads/logos/$id.$ext"
            }
            part.dispose()
        }
        val url = logoUrl ?: throw IllegalArgumentException("No image file provided")
        val club = clubRepo.updateLogoUrl(id, url)
        call.respond(club)
    }
}
