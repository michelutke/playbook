package com.playbook.di

import com.playbook.db.repositories.ClubRepositoryImpl
import com.playbook.db.repositories.CoachLinkRepositoryImpl
import com.playbook.db.repositories.EventRepositoryImpl
import com.playbook.db.repositories.InviteRepositoryImpl
import com.playbook.db.repositories.MembershipRepositoryImpl
import com.playbook.db.repositories.SubgroupRepositoryImpl
import com.playbook.db.repositories.SuperAdminRepositoryImpl
import com.playbook.db.repositories.TeamRepositoryImpl
import com.playbook.repository.ClubRepository
import com.playbook.repository.CoachLinkRepository
import com.playbook.repository.EventRepository
import com.playbook.repository.InviteRepository
import com.playbook.repository.MembershipRepository
import com.playbook.repository.SubgroupRepository
import com.playbook.repository.TeamRepository
import org.koin.dsl.module

val sharedModule = module {
    single<ClubRepository> { ClubRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
    single<MembershipRepository> { MembershipRepositoryImpl() }
    single<InviteRepository> { InviteRepositoryImpl(get(), get()) } // Mailer + config
    single<CoachLinkRepository> { CoachLinkRepositoryImpl() }
    single<EventRepository> { EventRepositoryImpl() }
    single<SubgroupRepository> { SubgroupRepositoryImpl() }
    single { SuperAdminRepositoryImpl(get(), get()) } // Mailer + config
}
