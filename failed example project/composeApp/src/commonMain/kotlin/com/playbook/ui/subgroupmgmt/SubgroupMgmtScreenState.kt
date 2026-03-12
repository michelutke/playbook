package com.playbook.ui.subgroupmgmt

import com.playbook.domain.Subgroup

data class SubgroupMgmtScreenState(
    val subgroups: List<Subgroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showSheet: Boolean = false,
    val editingSubgroupId: String? = null,
    val sheetName: String = "",
)
