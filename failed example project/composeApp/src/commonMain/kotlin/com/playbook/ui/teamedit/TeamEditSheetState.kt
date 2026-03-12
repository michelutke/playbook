package com.playbook.ui.teamedit

data class TeamEditSheetState(
    val name: String = "",
    val description: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)
