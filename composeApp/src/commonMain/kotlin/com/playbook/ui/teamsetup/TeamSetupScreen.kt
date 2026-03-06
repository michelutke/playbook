package com.playbook.ui.teamsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TeamSetupScreen(
    clubId: String,
    onSubmitted: () -> Unit,
    viewModel: TeamSetupViewModel = kmpViewModel(key = clubId) { parametersOf(clubId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TeamSetupEvent.Submitted -> onSubmitted()
            }
        }
    }
    TeamSetupContent(state = state, onAction = viewModel::submitAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSetupContent(
    state: TeamSetupScreenState,
    onAction: (TeamSetupAction) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Your Team") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Create your first team", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(TeamSetupAction.NameChanged(it)) },
                label = { Text("Team name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { onAction(TeamSetupAction.DescChanged(it)) },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { onAction(TeamSetupAction.Submit) },
                enabled = state.isFormValid && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Submit for Approval")
                }
            }

            Text(
                "Your team will be visible once approved by the club manager",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
