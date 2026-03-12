package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.club.ClubSetupViewModel
import com.playbook.ui.invite.InviteViewModel
import com.playbook.ui.team.TeamRosterViewModel
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect val uiModule: Module
