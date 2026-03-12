package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual val uiModule = module {
    viewModel { AuthViewModel(get()) }
    viewModel { params -> LoginViewModel(get(), onLoginSuccess = params.get()) }
    viewModel { params -> RegisterViewModel(get(), onRegisterSuccess = params.get()) }
    viewModel { EmptyStateViewModel(get()) }
}
