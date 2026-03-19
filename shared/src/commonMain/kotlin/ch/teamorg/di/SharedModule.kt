package ch.teamorg.di

import org.koin.core.module.Module

expect val sharedModule: Module

// TODO Plan 04: single<EventRepository> { EventRepositoryImpl(get()) }
