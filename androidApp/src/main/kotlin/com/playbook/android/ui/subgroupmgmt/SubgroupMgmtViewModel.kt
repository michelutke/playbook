package com.playbook.android.ui.subgroupmgmt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateSubgroupRequest
import com.playbook.domain.UpdateSubgroupRequest
import com.playbook.repository.SubgroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubgroupMgmtViewModel(
    private val teamId: String,
    private val subgroupRepository: SubgroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SubgroupMgmtScreenState())
    val state: StateFlow<SubgroupMgmtScreenState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: SubgroupMgmtAction) {
        when (action) {
            SubgroupMgmtAction.Refresh -> load()
            is SubgroupMgmtAction.EditSelected -> {
                val subgroup = _state.value.subgroups.find { it.id == action.subgroupId } ?: return
                _state.update { it.copy(showSheet = true, editingSubgroupId = subgroup.id, sheetName = subgroup.name) }
            }
            SubgroupMgmtAction.CreateSelected ->
                _state.update { it.copy(showSheet = true, editingSubgroupId = null, sheetName = "") }
            SubgroupMgmtAction.DismissSheet ->
                _state.update { it.copy(showSheet = false, editingSubgroupId = null, sheetName = "") }
            is SubgroupMgmtAction.SheetNameChanged -> _state.update { it.copy(sheetName = action.name) }
            SubgroupMgmtAction.SubmitSheet -> submitSheet()
            is SubgroupMgmtAction.DeleteConfirmed -> deleteSubgroup(action.subgroupId)
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val subgroups = subgroupRepository.listForTeam(teamId)
                _state.update { it.copy(subgroups = subgroups, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load sub-groups.") }
            }
        }
    }

    private fun submitSheet() {
        val s = _state.value
        val name = s.sheetName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                if (s.editingSubgroupId != null) {
                    subgroupRepository.update(s.editingSubgroupId, UpdateSubgroupRequest(name = name))
                } else {
                    subgroupRepository.create(teamId, CreateSubgroupRequest(name = name))
                }
                _state.update { it.copy(showSheet = false, editingSubgroupId = null, sheetName = "") }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save sub-group.") }
            }
        }
    }

    private fun deleteSubgroup(subgroupId: String) {
        viewModelScope.launch {
            try {
                subgroupRepository.delete(subgroupId)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete sub-group.") }
            }
        }
    }
}
