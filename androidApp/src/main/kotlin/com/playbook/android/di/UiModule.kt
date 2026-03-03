package com.playbook.android.di

import com.playbook.android.ui.absences.AbsenceSheetViewModel
import com.playbook.android.ui.absences.MyAbsencesViewModel
import com.playbook.android.ui.login.LoginViewModel
import com.playbook.android.ui.attendance.AttendanceListViewModel
import com.playbook.android.ui.clubcoachinvite.ClubCoachInviteViewModel
import com.playbook.android.ui.clubdashboard.ClubDashboardViewModel
import com.playbook.android.ui.clubedit.ClubEditViewModel
import com.playbook.android.ui.clubsetup.ClubSetupViewModel
import com.playbook.android.ui.eventcalendar.EventCalendarViewModel
import com.playbook.android.ui.eventdetail.EventDetailViewModel
import com.playbook.android.ui.eventform.EventFormViewModel
import com.playbook.android.ui.eventlist.EventListViewModel
import com.playbook.android.ui.inviteaccept.InviteAcceptViewModel
import com.playbook.android.ui.playerprofile.PlayerProfileViewModel
import com.playbook.android.ui.stats.PlayerStatsViewModel
import com.playbook.android.ui.stats.TeamStatsViewModel
import com.playbook.android.ui.subgroupmgmt.SubgroupMgmtViewModel
import com.playbook.android.ui.teamdetail.TeamDetailViewModel
import com.playbook.android.ui.teamedit.TeamEditViewModel
import com.playbook.android.ui.teaminvite.TeamInviteViewModel
import com.playbook.android.ui.notifications.NotificationInboxViewModel
import com.playbook.android.ui.notifications.NotificationSettingsViewModel
import com.playbook.android.ui.teamsetup.TeamSetupViewModel
import com.playbook.domain.RecurringScope
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val uiModule = module {
    factory { LoginViewModel(get(), get()) }
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
    // Event scheduling
    factory { (teamId: String?) -> EventListViewModel(teamId, get()) }
    factory { (teamId: String?) -> EventCalendarViewModel(teamId, get()) }
    factory { (eventId: String) -> EventDetailViewModel(eventId, get(), get()) }
    factory { (clubId: String, eventId: String?, preselectedTeamId: String?, editScope: RecurringScope) ->
        EventFormViewModel(clubId, eventId, preselectedTeamId, editScope, get(), get(), get())
    }
    factory { (teamId: String) -> SubgroupMgmtViewModel(teamId, get()) }
    // Attendance tracking
    factory { (eventId: String) -> AttendanceListViewModel(eventId, get()) }
    factory { MyAbsencesViewModel(get()) }
    factory { AbsenceSheetViewModel(get()) }
    factory { (userId: String, teamId: String?) -> PlayerStatsViewModel(userId, teamId, get()) }
    factory { (teamId: String) -> TeamStatsViewModel(teamId, get()) }
    // Notifications
    factory { NotificationInboxViewModel(get()) }
    factory { NotificationSettingsViewModel(get()) }
}
