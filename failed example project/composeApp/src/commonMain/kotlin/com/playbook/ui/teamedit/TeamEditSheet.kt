package com.playbook.ui.teamedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamEditSheet(
    teamId: String,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: TeamEditViewModel = kmpViewModel(key = teamId) { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TeamEditEvent.Saved -> onSaved()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Edit Team", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.submitAction(TeamEditAction.NameChanged(it)) },
                    label = { Text("Team name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.submitAction(TeamEditAction.DescChanged(it)) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                )

                val editError = state.error
                if (editError != null) {
                    Text(
                        text = editError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = { viewModel.submitAction(TeamEditAction.Save) },
                    enabled = state.name.isNotBlank() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}
