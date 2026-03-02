package com.playbook.di

import com.playbook.infra.DatabaseFactory
import com.playbook.infra.MailerFactory
import io.ktor.server.config.*
import org.koin.dsl.module

fun serverModule(config: ApplicationConfig) = module {
    single {
        DatabaseFactory.init(config)
        config // expose config for other components
    }
    single { MailerFactory.create(config) }
}
