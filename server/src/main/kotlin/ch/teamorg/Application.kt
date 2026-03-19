package ch.teamorg

import ch.teamorg.infra.DatabaseFactory
import ch.teamorg.infra.startMaterialisationJob
import ch.teamorg.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(environment.config)

    configureKoin()
    configureSerialization()
    configureStaticFiles()
    configureAuth()
    configureRouting()
    startMaterialisationJob()
}
