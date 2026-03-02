package com.playbook.di

import com.playbook.data.network.ApiConfig
import com.playbook.data.network.createHttpClient
import com.playbook.data.repository.*
import com.playbook.repository.*
import org.koin.dsl.module

fun sharedModule(apiConfig: ApiConfig) = module {
    single { createHttpClient() }
    single { apiConfig }
    single<ClubRepository> { ClubRepositoryImpl(get(), get()) }
    single<TeamRepository> { TeamRepositoryImpl(get(), get()) }
    single<MembershipRepository> { MembershipRepositoryImpl(get(), get()) }
    single<InviteRepository> { InviteRepositoryImpl(get(), get()) }
    single<CoachLinkRepository> { CoachLinkRepositoryImpl(get(), get()) }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    single<SubgroupRepository> { SubgroupRepositoryImpl(get(), get()) }
}
