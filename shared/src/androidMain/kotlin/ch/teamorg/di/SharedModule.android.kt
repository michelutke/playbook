package ch.teamorg.di

import ch.teamorg.data.DatabaseDriverFactory
import ch.teamorg.data.EventCacheManager
import ch.teamorg.data.createDatabase
import ch.teamorg.data.network.HttpClientFactory
import ch.teamorg.data.repository.AuthRepositoryImpl
import ch.teamorg.data.repository.ClubRepositoryImpl
import ch.teamorg.data.repository.EventRepositoryImpl
import ch.teamorg.data.repository.InviteRepositoryImpl
import ch.teamorg.data.repository.TeamRepositoryImpl
import ch.teamorg.preferences.UserPreferences
import ch.teamorg.repository.AuthRepository
import ch.teamorg.repository.ClubRepository
import ch.teamorg.repository.EventRepository
import ch.teamorg.repository.InviteRepository
import ch.teamorg.repository.TeamRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val sharedModule = module {
    single { UserPreferences(get()) }
    single { HttpClientFactory.create(get()) }
    single { DatabaseDriverFactory(get()) }
    single { createDatabase(get()) }
    singleOf(::EventCacheManager)
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::ClubRepositoryImpl) bind ClubRepository::class
    singleOf(::TeamRepositoryImpl) bind TeamRepository::class
    singleOf(::InviteRepositoryImpl) bind InviteRepository::class
    singleOf(::EventRepositoryImpl) bind EventRepository::class
}
