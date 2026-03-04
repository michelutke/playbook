package com.playbook.ui.clubedit

data class ClubEditScreenState(
    val name: String = "",
    val sportType: String = "",
    val location: String = "",
    val logoUrl: String? = null,
    val sportTypeDropdownExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = name.isNotBlank() && sportType.isNotBlank()
}
