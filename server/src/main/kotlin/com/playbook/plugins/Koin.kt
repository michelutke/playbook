package com.playbook.plugins

import com.playbook.di.StorageModule
import com.playbook.domain.repositories.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<ClubRepository> { ClubRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule, StorageModule)
    }
}
