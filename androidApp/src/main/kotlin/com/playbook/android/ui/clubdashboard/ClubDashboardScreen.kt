package com.playbook.android.ui.clubdashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ClubDashboardScreen(
    clubId: String,
    onNavigateToTeam: (teamId: String) -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToInviteCoaches: () -> Unit,
    viewModel: ClubDashboardViewModel = koinViewModel { parametersOf(clubId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ClubDashboardEvent.NavigateToTeam -> onNavigateToTeam(event.teamId)
                is ClubDashboardEvent.NavigateToEdit -> onNavigateToEdit()
                is ClubDashboardEvent.NavigateToInviteCoaches -> onNavigateToInviteCoaches()
            }
        }
    }
    ClubDashboardContent(
        state = state,
        onAction = viewModel::submitAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubDashboardContent(
    state: ClubDashboardScreenState,
    onAction: (ClubDashboardAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.club?.name ?: "Dashboard") },
                navigationIcon = {
                    if (state.club?.logoUrl != null) {
                        AsyncImage(
                            model = state.club.logoUrl,
                            contentDescription = "Club logo",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(ClubDashboardAction.NavigateToEdit) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit club")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(ClubDashboardAction.ShowCreateTeamSheet) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Create team")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.pendingTeams.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Pending Approval", style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.pendingTeams) { summary ->
                        PendingTeamCard(summary = summary, onAction = onAction)
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Teams", style = MaterialTheme.typography.titleMedium)
                }
                items(state.activeTeams) { summary ->
                    ActiveTeamCard(summary = summary, onAction = onAction)
                }

                if (state.archivedTeams.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Archived", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onAction(ClubDashboardAction.ToggleArchivedTeams) }) {
                                Icon(
                                    imageVector = if (state.showArchivedTeams) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = "Toggle archived",
                                )
                            }
                        }
                    }
                    if (state.showArchivedTeams) {
                        items(state.archivedTeams) { summary ->
                            ArchivedTeamCard(summary = summary)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onAction(ClubDashboardAction.NavigateToInviteCoaches) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Invite Coaches")
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        if (state.showCreateTeamSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { onAction(ClubDashboardAction.DismissCreateTeamSheet) },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Create Team", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = state.newTeamName,
                        onValueChange = { onAction(ClubDashboardAction.NewTeamNameChanged(it)) },
                        label = { Text("Team name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.newTeamDescription,
                        onValueChange = { onAction(ClubDashboardAction.NewTeamDescChanged(it)) },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                    )
                    Button(
                        onClick = { onAction(ClubDashboardAction.CreateTeam) },
                        enabled = state.newTeamName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Create")
                    }
                }
            }
        }

        if (state.pendingRejectionTeamId != null) {
            AlertDialog(
                onDismissRequest = { onAction(ClubDashboardAction.DismissRejectDialog) },
                title = { Text("Reject Team") },
                text = {
                    OutlinedTextField(
                        value = state.rejectionReason,
                        onValueChange = { onAction(ClubDashboardAction.RejectionReasonChanged(it)) },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Button(onClick = { onAction(ClubDashboardAction.RejectTeam(state.pendingRejectionTeamId)) }) {
                        Text("Reject")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(ClubDashboardAction.DismissRejectDialog) }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun PendingTeamCard(summary: TeamSummary, onAction: (ClubDashboardAction) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary.team.name, style = MaterialTheme.typography.titleMedium)
            if (summary.team.requestedBy != null) {
                Text(
                    "Requested by ${summary.team.requestedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAction(ClubDashboardAction.ApproveTeam(summary.team.id)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = { onAction(ClubDashboardAction.ShowRejectDialog(summary.team.id)) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun ActiveTeamCard(summary: TeamSummary, onAction: (ClubDashboardAction) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAction(ClubDashboardAction.NavigateToTeam(summary.team.id)) },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary.team.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${summary.memberCount} members",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.coachAvatarUrls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box {
                    summary.coachAvatarUrls.forEachIndexed { index, url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = (index * 18).dp)
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedTeamCard(summary: TeamSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                summary.team.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "Archived",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
