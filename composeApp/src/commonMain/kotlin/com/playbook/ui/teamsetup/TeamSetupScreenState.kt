package com.playbook.ui.teamsetup

data class TeamSetupScreenState(
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = name.isNotBlank()
}
