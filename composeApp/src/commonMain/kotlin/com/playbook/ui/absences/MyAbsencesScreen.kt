package com.playbook.ui.absences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.domain.AbwesenheitRule
import com.playbook.domain.AbwesenheitRuleType
import com.playbook.di.kmpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAbsencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyAbsencesViewModel = kmpViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.backfillSnackbar) {
        when (val s = state.backfillSnackbar) {
            BackfillSnackbarState.Hidden -> snackbarHostState.currentSnackbarData?.dismiss()
            BackfillSnackbarState.Pending -> snackbarHostState.showSnackbar(
                message = "Applying absence rules…",
                duration = SnackbarDuration.Indefinite,
            )
            BackfillSnackbarState.Done -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Absence rules applied",
                    duration = SnackbarDuration.Short,
                )
            }
            BackfillSnackbarState.Failed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "Failed to apply rules — tap to retry",
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Indefinite,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.retryBackfill()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Absences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.submitAction(MyAbsencesAction.AddTapped) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add absence")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.rules.isEmpty() -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No absences declared", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { viewModel.submitAction(MyAbsencesAction.AddTapped) }) {
                        Text("Add absence")
                    }
                }
            }
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(state.rules, key = { it.id }) { rule ->
                    SwipeableRuleRow(
                        rule = rule,
                        onEdit = { viewModel.submitAction(MyAbsencesAction.EditTapped(rule)) },
                        onDelete = { viewModel.submitAction(MyAbsencesAction.DeleteTapped(rule.id)) },
                    )
                }
            }
        }
    }

    if (state.showAddSheet || state.editingRule != null) {
        AbsenceSheet(
            existingRule = state.editingRule,
            onSaved = { viewModel.onSaved() },
            onDismiss = { viewModel.submitAction(MyAbsencesAction.SheetDismissed) },
        )
    }

    if (state.showDeleteConfirmFor != null) {
        AlertDialog(
            onDismissRequest = { viewModel.submitAction(MyAbsencesAction.DismissDeleteConfirm) },
            title = { Text("Delete absence rule?") },
            text = { Text("This will remove the absence rule. Future events will no longer be auto-declined.") },
            confirmButton = {
                Button(onClick = { viewModel.submitAction(MyAbsencesAction.ConfirmDelete) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.submitAction(MyAbsencesAction.DismissDeleteConfirm) }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRuleRow(
    rule: AbwesenheitRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            RuleRow(rule = rule, onClick = onEdit)
        }
    }
}

@Composable
private fun RuleRow(rule: AbwesenheitRule, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rule.presetType.icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.label, style = MaterialTheme.typography.bodyMedium)
                val chipText = when (rule.ruleType) {
                    AbwesenheitRuleType.PERIOD -> {
                        val from = rule.startDate?.toString() ?: "?"
                        val to = rule.endDate?.toString() ?: "?"
                        "$from – $to"
                    }
                    AbwesenheitRuleType.RECURRING -> {
                        val days = rule.weekdays?.joinToString(", ") { dayName(it) } ?: "—"
                        "Every $days"
                    }
                }
                Text(chipText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun dayName(day: Int): String = when (day) {
    0 -> "Mon"; 1 -> "Tue"; 2 -> "Wed"; 3 -> "Thu"; 4 -> "Fri"; 5 -> "Sat"; 6 -> "Sun"
    else -> "?"
}
