package com.playbook.android.ui.clubedit

import android.net.Uri

data class ClubEditScreenState(
    val name: String = "",
    val sportType: String = "",
    val location: String = "",
    val logoUrl: String? = null,
    val logoUri: Uri? = null,
    val sportTypeDropdownExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = name.isNotBlank() && sportType.isNotBlank()
}
