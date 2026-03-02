package com.playbook.android.di

import com.playbook.android.ui.clubcoachinvite.ClubCoachInviteViewModel
import com.playbook.android.ui.clubdashboard.ClubDashboardViewModel
import com.playbook.android.ui.clubedit.ClubEditViewModel
import com.playbook.android.ui.clubsetup.ClubSetupViewModel
import com.playbook.android.ui.inviteaccept.InviteAcceptViewModel
import com.playbook.android.ui.playerprofile.PlayerProfileViewModel
import com.playbook.android.ui.teamdetail.TeamDetailViewModel
import com.playbook.android.ui.teamedit.TeamEditViewModel
import com.playbook.android.ui.teaminvite.TeamInviteViewModel
import com.playbook.android.ui.teamsetup.TeamSetupViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val uiModule = module {
    factory { ClubSetupViewModel(get(), androidApplication()) }
    factory { (clubId: String) -> ClubDashboardViewModel(clubId, get(), get(), get()) }
    factory { (teamId: String, clubId: String) -> TeamDetailViewModel(teamId, clubId, get(), get()) }
    factory { (clubId: String) -> ClubEditViewModel(clubId, get(), androidApplication()) }
    factory { (teamId: String) -> TeamEditViewModel(teamId, get()) }
    factory { (teamId: String) -> TeamInviteViewModel(teamId, get(), get()) }
    factory { (clubId: String) -> ClubCoachInviteViewModel(clubId, get(), get(), get()) }
    factory { (teamId: String, userId: String) -> PlayerProfileViewModel(teamId, userId, get()) }
    factory { (clubId: String) -> TeamSetupViewModel(clubId, get()) }
    factory { (token: String) -> InviteAcceptViewModel(token, get()) }
}
