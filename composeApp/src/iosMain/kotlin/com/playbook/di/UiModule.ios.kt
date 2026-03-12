package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterViewModel
import org.koin.dsl.module

actual val uiModule = module {
    factory { AuthViewModel(get()) }
    factory { params -> LoginViewModel(get(), onLoginSuccess = params.get()) }
    factory { params -> RegisterViewModel(get(), onRegisterSuccess = params.get()) }
    factory { EmptyStateViewModel(get()) }
}
