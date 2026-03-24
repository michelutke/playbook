package ch.teamorg.ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.teamorg.domain.SubGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubGroupSheet(
    teamId: String,
    subGroups: List<SubGroup>,
    isCoachOrManager: Boolean,
    onDismiss: () -> Unit,
    onCreateSubGroup: (String) -> Unit,
    onDeleteSubGroup: (String) -> Unit
) {
    var showAddField by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Sub-groups",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (subGroups.isEmpty() && !showAddField) {
                Text(
                    "No sub-groups yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subGroups, key = { it.id }) { subGroup ->
                    SubGroupRow(
                        subGroup = subGroup,
                        isCoachOrManager = isCoachOrManager,
                        onDelete = { onDeleteSubGroup(subGroup.id) }
                    )
                }
            }

            if (showAddField) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder = { Text("Sub-group name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            TextButton(
                                onClick = {
                                    if (newGroupName.isNotBlank()) {
                                        onCreateSubGroup(newGroupName.trim())
                                        newGroupName = ""
                                        showAddField = false
                                    }
                                }
                            ) { Text("Add") }
                            TextButton(onClick = { showAddField = false; newGroupName = "" }) { Text("Cancel") }
                        }
                    }
                )
            }

            if (isCoachOrManager && !showAddField) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showAddField = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Sub-group")
                }
            }
        }
    }
}

@Composable
private fun SubGroupRow(
    subGroup: SubGroup,
    isCoachOrManager: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subGroup.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${subGroup.memberCount} member${if (subGroup.memberCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCoachOrManager) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete sub-group",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Sub-group") },
            text = { Text("Delete \"${subGroup.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
