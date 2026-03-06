package com.playbook.di

import com.playbook.db.repositories.NotificationRepositoryImpl
import com.playbook.infra.DatabaseFactory
import com.playbook.infra.MailerFactory
import com.playbook.push.NotificationService
import com.playbook.push.OneSignalPushService
import com.playbook.push.PushService
import io.ktor.server.config.*
import org.koin.dsl.module

fun serverModule(config: ApplicationConfig) = module {
    single(createdAtStart = true) {
        DatabaseFactory.init(config)
        config // expose config for other components
    }
    single { MailerFactory.create(config) }
    single<PushService> { OneSignalPushService() }
    single { NotificationService(get()) }
    single { NotificationRepositoryImpl() }
}
