package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.club.ClubSetupViewModel
import com.playbook.ui.invite.InviteViewModel
import com.playbook.ui.team.TeamRosterViewModel
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterViewModel
import org.koin.dsl.module

actual val uiModule = module {
    factory { AuthViewModel(get()) }
    factory { LoginViewModel(get()) }
    factory { RegisterViewModel(get()) }
    factory { EmptyStateViewModel(get()) }
    factory { ClubSetupViewModel(get()) }
    factory { TeamRosterViewModel(get()) }
    factory { InviteViewModel(get()) }
}
