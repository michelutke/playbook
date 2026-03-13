package com.playbook.ui.team

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.playbook.domain.TeamMember
import com.playbook.ui.theme.PlaybookTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRosterScreen(
    teamId: String,
    viewModel: TeamRosterViewModel,
    onBack: () -> Unit,
    onShareInvite: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var memberToRemove by remember { mutableStateOf<TeamMember?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        viewModel.loadRoster(teamId)
    }

    LaunchedEffect(state.inviteUrl) {
        state.inviteUrl?.let { url ->
            onShareInvite(url)
            viewModel.resetInvite()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Roster") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showInviteDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Invite Player")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && !state.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.members.isEmpty() && !state.isLoading) {
                Text(
                    "No members in this team yet.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // Simplified PullToRefresh (using standard Box and state)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.members) { member ->
                        MemberItem(
                            member = member,
                            onLongClick = { memberToRemove = member }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove ${member.displayName} from the team?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(teamId, member.userId)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Invite Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite to Team") },
            text = { Text("What role would you like to invite?") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.createInvite(teamId, "player")
                            showInviteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Invite as Player")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.createInvite(teamId, "coach")
                            showInviteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Invite as Coach")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberItem(
    member: TeamMember,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = {}),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${member.role.replaceFirstChar { it.uppercase() }}${member.position?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (member.jerseyNumber != null) {
                Text(
                    text = "#${member.jerseyNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}
