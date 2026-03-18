package ch.teamorg.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureStaticFiles() {
    routing {
        static("/uploads") {
            files(File(System.getenv("UPLOADS_DIR") ?: "uploads"))
        }
    }
}
