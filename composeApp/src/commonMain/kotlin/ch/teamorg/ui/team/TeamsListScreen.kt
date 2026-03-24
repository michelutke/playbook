package ch.teamorg.ui.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsListScreen(
    viewModel: TeamsListViewModel,
    onTeamClick: (teamId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.club?.name ?: "Teams") },
                actions = {
                    if (state.isClubManager) {
                        IconButton(onClick = { viewModel.showEditClubSheet() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Club")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isClubManager) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateSheet() },
                    modifier = Modifier.testTag("fab_create_team")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Team")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Unknown error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.teams.isEmpty() -> {
                    Text(
                        text = "No teams yet. Create your first team.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.teams, key = { it.id }) { team ->
                            TeamCard(
                                name = team.name,
                                memberCount = team.memberCount,
                                onClick = { onTeamClick(team.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showCreateSheet) {
        TeamEditSheet(
            isCreate = true,
            onSave = { name, description -> viewModel.createTeam(name, description) },
            onDismiss = { viewModel.hideCreateSheet() }
        )
    }

    if (state.showEditClubSheet) {
        ClubEditSheet(
            currentName = state.club?.name ?: "",
            currentLocation = state.club?.location ?: "",
            onSave = { name, location -> viewModel.updateClub(name, location) },
            onDismiss = { viewModel.hideEditClubSheet() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubEditSheet(
    currentName: String,
    currentLocation: String,
    onSave: (name: String, location: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var location by remember { mutableStateOf(currentLocation) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Club",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Club name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), location.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun TeamCard(
    name: String,
    memberCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$memberCount member${if (memberCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
