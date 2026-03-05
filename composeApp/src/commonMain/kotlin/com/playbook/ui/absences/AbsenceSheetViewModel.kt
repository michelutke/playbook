package com.playbook.ui.absences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateAbwesenheitRuleRequest
import com.playbook.domain.UpdateAbwesenheitRuleRequest
import com.playbook.repository.AbwesenheitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SaveState { Idle, Saving, Success, Error }

class AbsenceSheetViewModel(
    private val abwesenheitRepository: AbwesenheitRepository,
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun createRule(request: CreateAbwesenheitRuleRequest) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                abwesenheitRepository.createRule("", request)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error
            }
        }
    }

    fun updateRule(ruleId: String, request: UpdateAbwesenheitRuleRequest) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                abwesenheitRepository.updateRule(ruleId, "", request)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
