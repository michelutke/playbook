package com.playbook.di

import com.playbook.auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { AuthViewModel(get()) }
}
