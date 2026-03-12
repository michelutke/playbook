package com.playbook.ui.clubsetup

sealed class ClubSetupAction {
    data class NameChanged(val name: String) : ClubSetupAction()
    data class SportTypeChanged(val sportType: String) : ClubSetupAction()
    data class LocationChanged(val location: String) : ClubSetupAction()
    data object SportTypeDropdownToggled : ClubSetupAction()
    data object Submit : ClubSetupAction()
}
