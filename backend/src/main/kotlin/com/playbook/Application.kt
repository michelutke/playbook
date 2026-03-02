package com.playbook

import com.playbook.infra.startMaterializationJob
import com.playbook.plugins.configureAuth
import com.playbook.plugins.configureKoin
import com.playbook.plugins.configureRouting
import com.playbook.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureAuth()
    configureKoin()
    configureRouting()
    startMaterializationJob()
}
