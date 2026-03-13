package com.playbook.di

import com.playbook.data.network.HttpClientFactory
import com.playbook.data.repository.AuthRepositoryImpl
import com.playbook.data.repository.ClubRepositoryImpl
import com.playbook.data.repository.InviteRepositoryImpl
import com.playbook.data.repository.TeamRepositoryImpl
import com.playbook.preferences.UserPreferences
import com.playbook.repository.AuthRepository
import com.playbook.repository.ClubRepository
import com.playbook.repository.InviteRepository
import com.playbook.repository.TeamRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val sharedModule = module {
    single { UserPreferences(get()) }
    single { HttpClientFactory.create(get()) }
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::ClubRepositoryImpl) bind ClubRepository::class
    singleOf(::TeamRepositoryImpl) bind TeamRepository::class
    singleOf(::InviteRepositoryImpl) bind InviteRepository::class
}
