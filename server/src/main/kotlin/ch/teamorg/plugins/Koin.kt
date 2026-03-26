package ch.teamorg.plugins

import ch.teamorg.di.StorageModule
import ch.teamorg.domain.repositories.*
import ch.teamorg.infra.AbwesenheitBackfillJob
import ch.teamorg.infra.NotificationDispatcher
import ch.teamorg.infra.PushService
import ch.teamorg.infra.PushServiceImpl
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val appModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<ClubRepository> { ClubRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
    single<InviteRepository> { InviteRepositoryImpl() }
    single<EventRepository> { EventRepositoryImpl() }
    single<AttendanceRepository> { AttendanceRepositoryImpl() }
    single<AbwesenheitRepository> { AbwesenheitRepositoryImpl() }
    single { AbwesenheitBackfillJob() }
    single<PushService> {
        PushServiceImpl(HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        })
    }
    single<NotificationRepository> { NotificationRepositoryImpl() }
    single { NotificationDispatcher(get(), get()) }
}

fun Application.configureKoin() {
    install(Koin) {
        printLogger(Level.INFO)
        modules(appModule, StorageModule)
    }
}
