package com.playbook.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.playbook.db.PlaybookDatabase
import org.koin.dsl.module

fun iosPlatformModule() = module {
    single {
        NativeSqliteDriver(
            schema = PlaybookDatabase.Schema,
            name = "playbook.db",
        )
    }
}
