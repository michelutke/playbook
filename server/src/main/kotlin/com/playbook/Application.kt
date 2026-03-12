package com.playbook

import com.playbook.infra.DatabaseFactory
import com.playbook.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(environment.config)
    
    configureSerialization()
    configureAuth()
    // Koin and Routing will be configured as they are implemented
    configureRouting()
}
