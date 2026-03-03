package com.playbook.android.ui.absences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.repository.AbwesenheitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyAbsencesViewModel(
    private val abwesenheitRepository: AbwesenheitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyAbsencesScreenState())
    val state: StateFlow<MyAbsencesScreenState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        load()
    }

    fun submitAction(action: MyAbsencesAction) {
        when (action) {
            MyAbsencesAction.Refresh -> load()
            MyAbsencesAction.AddTapped -> _state.update { it.copy(showAddSheet = true, editingRule = null) }
            is MyAbsencesAction.EditTapped -> _state.update { it.copy(editingRule = action.rule, showAddSheet = false) }
            is MyAbsencesAction.DeleteTapped -> _state.update { it.copy(showDeleteConfirmFor = action.ruleId) }
            MyAbsencesAction.ConfirmDelete -> {
                val ruleId = _state.value.showDeleteConfirmFor ?: return
                _state.update { it.copy(showDeleteConfirmFor = null) }
                deleteRule(ruleId)
            }
            MyAbsencesAction.DismissDeleteConfirm -> _state.update { it.copy(showDeleteConfirmFor = null) }
            MyAbsencesAction.SheetDismissed -> _state.update { it.copy(showAddSheet = false, editingRule = null) }
        }
    }

    fun onSaved() {
        _state.update { it.copy(showAddSheet = false, editingRule = null) }
        load()
        startBackfillPoll()
    }

    fun retryBackfill() {
        startBackfillPoll()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val rules = abwesenheitRepository.listRules("")
                _state.update { it.copy(rules = rules, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load absences.") }
            }
        }
    }

    private fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            try {
                abwesenheitRepository.deleteRule(ruleId, "")
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete absence rule.") }
            }
        }
    }

    private fun startBackfillPoll() {
        pollJob?.cancel()
        _state.update { it.copy(backfillSnackbar = BackfillSnackbarState.Pending) }
        pollJob = viewModelScope.launch {
            try {
                abwesenheitRepository.pollBackfillStatus().collect { status ->
                    when (status.status) {
                        "pending" -> _state.update { it.copy(backfillSnackbar = BackfillSnackbarState.Pending) }
                        "done" -> {
                            _state.update { it.copy(backfillSnackbar = BackfillSnackbarState.Done) }
                            pollJob?.cancel()
                        }
                        else -> {
                            _state.update { it.copy(backfillSnackbar = BackfillSnackbarState.Failed) }
                            pollJob?.cancel()
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(backfillSnackbar = BackfillSnackbarState.Failed) }
            }
        }
    }
}
