package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.clubsetup.ClubSetupViewModel
import com.playbook.ui.inviteaccept.InviteAcceptViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.teamsetup.TeamSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { AuthViewModel(get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ClubSetupViewModel(get(), get()) }
    viewModel { (clubId: String) -> TeamSetupViewModel(clubId, get()) }
    viewModel { (token: String) -> InviteAcceptViewModel(token, get()) }
}
