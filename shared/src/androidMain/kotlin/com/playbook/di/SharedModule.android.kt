package com.playbook.di

import com.playbook.data.network.HttpClientFactory
import com.playbook.data.repository.AuthRepositoryImpl
import com.playbook.preferences.UserPreferences
import com.playbook.repository.AuthRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val sharedModule = module {
    single { UserPreferences(get()) }
    single { HttpClientFactory.create(get()) }
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
}
