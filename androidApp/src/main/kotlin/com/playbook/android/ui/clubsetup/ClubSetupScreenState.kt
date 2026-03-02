package com.playbook.android.ui.clubsetup

import android.net.Uri

data class ClubSetupScreenState(
    val name: String = "",
    val sportType: String = "",
    val location: String = "",
    val logoUri: Uri? = null,
    val sportTypeDropdownExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = name.isNotBlank() && sportType.isNotBlank()
}
