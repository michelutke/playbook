package com.playbook.android.di

import com.playbook.android.ui.clubsetup.ClubSetupViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val uiModule = module {
    factory { ClubSetupViewModel(get(), androidApplication()) }
}
