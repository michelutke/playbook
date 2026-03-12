package com.playbook.ui.clubsetup

data class ClubSetupScreenState(
    val name: String = "",
    val sportType: String = "",
    val location: String = "",
    val sportTypeDropdownExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = name.isNotBlank() && sportType.isNotBlank()
}
