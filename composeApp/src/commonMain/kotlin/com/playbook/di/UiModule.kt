package com.playbook.di

import com.playbook.auth.AuthViewModel
import com.playbook.ui.clubcoachinvite.ClubCoachInviteViewModel
import com.playbook.ui.clubdashboard.ClubDashboardViewModel
import com.playbook.ui.clubedit.ClubEditViewModel
import com.playbook.ui.clubsetup.ClubSetupViewModel
import com.playbook.ui.inviteaccept.InviteAcceptViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.playerprofile.PlayerProfileViewModel
import com.playbook.ui.stats.PlayerStatsViewModel
import com.playbook.ui.stats.TeamStatsViewModel
import com.playbook.ui.subgroupmgmt.SubgroupMgmtViewModel
import com.playbook.ui.eventlist.EventListViewModel
import com.playbook.ui.teamdetail.TeamDetailViewModel
import com.playbook.ui.teamedit.TeamEditViewModel
import com.playbook.ui.teaminvite.TeamInviteViewModel
import com.playbook.ui.teamsetup.TeamSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { AuthViewModel(get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ClubSetupViewModel(get(), get()) }
    viewModel { (clubId: String) -> TeamSetupViewModel(clubId, get()) }
    viewModel { (token: String) -> InviteAcceptViewModel(token, get()) }
    viewModel { (clubId: String) -> ClubDashboardViewModel(clubId, get(), get(), get()) }
    viewModel { (teamId: String, clubId: String) -> TeamDetailViewModel(teamId, clubId, get(), get()) }
    viewModel { (clubId: String) -> ClubEditViewModel(clubId, get()) }
    viewModel { (clubId: String) -> ClubCoachInviteViewModel(clubId, get(), get(), get()) }
    viewModel { (teamId: String) -> TeamEditViewModel(teamId, get()) }
    viewModel { (teamId: String) -> TeamInviteViewModel(teamId, get(), get()) }
    viewModel { (teamId: String, userId: String) -> PlayerProfileViewModel(teamId, userId, get()) }
    viewModel { (userId: String, teamId: String?) -> PlayerStatsViewModel(userId, teamId, get()) }
    viewModel { (teamId: String) -> TeamStatsViewModel(teamId, get()) }
    viewModel { (teamId: String) -> SubgroupMgmtViewModel(teamId, get()) }
    viewModel { (teamId: String?) -> EventListViewModel(teamId, get()) }
}
