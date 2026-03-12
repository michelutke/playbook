package com.playbook.di

import com.playbook.preferences.UserPreferences
import com.playbook.push.PushPermissionRequester
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidComposeModule = module {
    single { UserPreferences(androidContext()) }
    factory { PushPermissionRequester() }
}
