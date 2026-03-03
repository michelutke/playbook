package com.playbook.di

import com.playbook.attendance.MutationQueue
import com.playbook.data.network.ApiConfig
import com.playbook.data.network.createHttpClient
import com.playbook.data.repository.*
import com.playbook.db.PlaybookDatabase
import com.playbook.repository.*
import org.koin.dsl.module

fun sharedModule(apiConfig: ApiConfig) = module {
    single { createHttpClient() }
    single { apiConfig }
    single { PlaybookDatabase(get()) }
    single { get<PlaybookDatabase>().attendanceQueries }
    single { MutationQueue(get()) }
    single<ClubRepository> { ClubRepositoryImpl(get(), get()) }
    single<TeamRepository> { TeamRepositoryImpl(get(), get()) }
    single<MembershipRepository> { MembershipRepositoryImpl(get(), get()) }
    single<InviteRepository> { InviteRepositoryImpl(get(), get()) }
    single<CoachLinkRepository> { CoachLinkRepositoryImpl(get(), get()) }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    single<SubgroupRepository> { SubgroupRepositoryImpl(get(), get()) }
    single<AttendanceRepository> { AttendanceRepositoryImpl(get(), get(), get(), get()) }
    single<AbwesenheitRepository> { AbwesenheitRepositoryImpl(get(), get()) }
}
