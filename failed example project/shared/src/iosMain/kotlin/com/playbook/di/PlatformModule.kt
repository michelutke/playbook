package com.playbook.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.playbook.db.PlaybookDatabase
import org.koin.dsl.module

fun iosPlatformModule() = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = PlaybookDatabase.Schema,
            name = "playbook.db",
        )
    }
}
