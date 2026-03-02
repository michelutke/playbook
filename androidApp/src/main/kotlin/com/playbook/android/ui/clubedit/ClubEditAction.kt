package com.playbook.android.ui.clubedit

import android.net.Uri

sealed class ClubEditAction {
    data class NameChanged(val name: String) : ClubEditAction()
    data class SportTypeChanged(val sportType: String) : ClubEditAction()
    data class LocationChanged(val location: String) : ClubEditAction()
    data class LogoSelected(val uri: Uri) : ClubEditAction()
    data object SportTypeDropdownToggled : ClubEditAction()
    data object Save : ClubEditAction()
}
