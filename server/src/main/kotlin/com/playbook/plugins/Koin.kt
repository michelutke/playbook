package com.playbook.plugins

import com.playbook.di.StorageModule
import com.playbook.domain.repositories.*
import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val appModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<ClubRepository> { ClubRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
    single<InviteRepository> { InviteRepositoryImpl() }
}

fun Application.configureKoin() {
    install(Koin) {
        printLogger(Level.INFO)
        modules(appModule, StorageModule)
    }
}
