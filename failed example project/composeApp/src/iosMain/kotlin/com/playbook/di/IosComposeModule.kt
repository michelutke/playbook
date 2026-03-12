package com.playbook.di

import com.playbook.preferences.UserPreferences
import com.playbook.push.PushPermissionRequester
import org.koin.dsl.module

val iosComposeModule = module {
    single { UserPreferences() }
    factory { PushPermissionRequester() }
}
