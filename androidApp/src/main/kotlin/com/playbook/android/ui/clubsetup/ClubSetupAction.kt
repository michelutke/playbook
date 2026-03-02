package com.playbook.android.ui.clubsetup

import android.net.Uri

sealed class ClubSetupAction {
    data class NameChanged(val name: String) : ClubSetupAction()
    data class SportTypeChanged(val sportType: String) : ClubSetupAction()
    data class LocationChanged(val location: String) : ClubSetupAction()
    data class LogoSelected(val uri: Uri) : ClubSetupAction()
    data object SportTypeDropdownToggled : ClubSetupAction()
    data object Submit : ClubSetupAction()
}
