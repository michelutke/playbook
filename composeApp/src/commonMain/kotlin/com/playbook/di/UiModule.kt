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
import com.playbook.domain.RecurringScope
import com.playbook.ui.absences.AbsenceSheetViewModel
import com.playbook.ui.absences.MyAbsencesViewModel
import com.playbook.ui.attendancelist.AttendanceListViewModel
import com.playbook.ui.notifications.NotificationInboxViewModel
import com.playbook.ui.notifications.NotificationSettingsViewModel
import com.playbook.ui.eventcalendar.EventCalendarViewModel
import com.playbook.ui.eventdetail.EventDetailViewModel
import com.playbook.ui.eventform.EventFormViewModel
import com.playbook.ui.eventlist.EventListViewModel
import com.playbook.ui.teamdetail.TeamDetailViewModel
import com.playbook.ui.teamedit.TeamEditViewModel
import com.playbook.ui.teaminvite.TeamInviteViewModel
import com.playbook.ui.teamsetup.TeamSetupViewModel
import org.koin.dsl.module

val uiModule = module {
    single { AuthViewModel(get()) }
    factory { LoginViewModel(get(), get(), get()) }
    factory { ClubSetupViewModel(get(), get()) }
    factory { (clubId: String) -> TeamSetupViewModel(clubId, get()) }
    factory { (token: String) -> InviteAcceptViewModel(token, get()) }
    factory { (clubId: String) -> ClubDashboardViewModel(clubId, get(), get(), get()) }
    factory { (teamId: String, clubId: String) -> TeamDetailViewModel(teamId, clubId, get(), get()) }
    factory { (clubId: String) -> ClubEditViewModel(clubId, get()) }
    factory { (clubId: String) -> ClubCoachInviteViewModel(clubId, get(), get(), get()) }
    factory { (teamId: String) -> TeamEditViewModel(teamId, get()) }
    factory { (teamId: String) -> TeamInviteViewModel(teamId, get(), get()) }
    factory { (teamId: String, userId: String) -> PlayerProfileViewModel(teamId, userId, get()) }
    factory { (userId: String, teamId: String?) -> PlayerStatsViewModel(userId, teamId, get()) }
    factory { (teamId: String) -> TeamStatsViewModel(teamId, get()) }
    factory { (teamId: String) -> SubgroupMgmtViewModel(teamId, get()) }
    factory { (teamId: String?) -> EventListViewModel(teamId, get()) }
    factory { (teamId: String?) -> EventCalendarViewModel(teamId, get()) }
    factory { (eventId: String) -> EventDetailViewModel(eventId, get(), get()) }
    factory { (clubId: String, eventId: String?, preselectedTeamId: String?, editScope: RecurringScope) ->
        EventFormViewModel(clubId, eventId, preselectedTeamId, editScope, get(), get(), get())
    }
    factory { (eventId: String) -> AttendanceListViewModel(eventId, get()) }
    factory { MyAbsencesViewModel(get()) }
    factory { AbsenceSheetViewModel(get()) }
    factory { NotificationInboxViewModel(get()) }
    factory { NotificationSettingsViewModel(get()) }
}
