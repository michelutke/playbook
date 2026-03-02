package com.playbook.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.playbook.db.PlaybookDatabase
import org.koin.dsl.module

fun androidPlatformModule(context: Context) = module {
    single {
        AndroidSqliteDriver(
            schema = PlaybookDatabase.Schema,
            context = context,
            name = "playbook.db",
        )
    }
}
