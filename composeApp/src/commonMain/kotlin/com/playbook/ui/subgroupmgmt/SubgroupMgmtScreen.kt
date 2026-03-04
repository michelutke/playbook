package com.playbook.ui.subgroupmgmt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playbook.domain.Subgroup
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SubgroupMgmtScreen(
    teamId: String,
    onNavigateBack: () -> Unit,
    viewModel: SubgroupMgmtViewModel = koinViewModel { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsState()
    SubgroupMgmtContent(state = state, onAction = viewModel::submitAction, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubgroupMgmtContent(
    state: SubgroupMgmtScreenState,
    onAction: (SubgroupMgmtAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sub-groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(SubgroupMgmtAction.CreateSelected) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Create sub-group")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            state.subgroups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sub-groups yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(state.subgroups, key = { it.id }) { subgroup ->
                    SwipeToDeleteSubgroupItem(
                        subgroup = subgroup,
                        onClick = { onAction(SubgroupMgmtAction.EditSelected(subgroup.id)) },
                        onDelete = { onAction(SubgroupMgmtAction.DeleteConfirmed(subgroup.id)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (state.showSheet) {
        SubgroupSheet(
            editingId = state.editingSubgroupId,
            name = state.sheetName,
            onNameChanged = { onAction(SubgroupMgmtAction.SheetNameChanged(it)) },
            onSubmit = { onAction(SubgroupMgmtAction.SubmitSheet) },
            onDismiss = { onAction(SubgroupMgmtAction.DismissSheet) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteSubgroupItem(
    subgroup: Subgroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showConfirmDialog = true
                false
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(subgroup.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text("${subgroup.memberCount} members") },
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = subgroup.memberCount.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            },
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete sub-group") },
            text = { Text("\"${subgroup.name}\" will be deleted. Events targeting it will become team-wide.") },
            confirmButton = {
                Button(
                    onClick = { showConfirmDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubgroupSheet(
    editingId: String?,
    name: String,
    onNameChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = if (editingId != null) "Edit Sub-group" else "New Sub-group",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                ) {
                    Text(if (editingId != null) "Save" else "Create")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
