package com.playbook.ui.clubedit

sealed class ClubEditAction {
    data class NameChanged(val name: String) : ClubEditAction()
    data class SportTypeChanged(val sportType: String) : ClubEditAction()
    data class LocationChanged(val location: String) : ClubEditAction()
    data object SportTypeDropdownToggled : ClubEditAction()
    data object Save : ClubEditAction()
}
