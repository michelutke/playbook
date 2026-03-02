package com.playbook.plugins

import com.playbook.di.serverModule
import com.playbook.di.sharedModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(serverModule(environment.config), sharedModule)
    }
}
