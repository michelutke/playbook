package ch.teamorg.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import ch.teamorg.ui.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ch.teamorg.domain.TeamMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRosterScreen(
    teamId: String,
    viewModel: TeamRosterViewModel,
    onBack: () -> Unit,
    onShareInvite: (String) -> Unit,
    onMemberClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var memberToRemove by remember { mutableStateOf<TeamMember?>(null) }
    var memberToPromote by remember { mutableStateOf<TeamMember?>(null) }
    var memberAction by remember { mutableStateOf<TeamMember?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        viewModel.loadRoster(teamId)
    }

    Scaffold(
        modifier = Modifier.testTagsAsResourceId(),
        topBar = {
            TopAppBar(
                title = { Text(state.teamName.ifBlank { "Team Roster" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isClubManager) {
                        IconButton(
                            onClick = {
                                viewModel.loadSubGroups(teamId)
                                viewModel.toggleSubGroupSheet()
                            },
                            modifier = Modifier.testTag("btn_sub_groups")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Sub-groups")
                        }
                        IconButton(
                            onClick = { viewModel.showEditTeamSheet() },
                            modifier = Modifier.testTag("btn_edit_team")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Team")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInviteDialog = true },
                modifier = Modifier.testTag("fab_invite_player")
            ) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.members) { member ->
                        MemberItem(
                            member = member,
                            onClick = { onMemberClick(member.userId) },
                            onLongClick = {
                                if (state.isClubManager) {
                                    memberAction = member
                                } else {
                                    memberToRemove = member
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ClubManager member action dialog (promote / remove)
    memberAction?.let { member ->
        AlertDialog(
            onDismissRequest = { memberAction = null },
            title = { Text(member.displayName) },
            text = { Text("Choose an action for this member.") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (member.role == "player") {
                        Button(
                            onClick = {
                                memberToPromote = member
                                memberAction = null
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_promote_to_coach")
                        ) {
                            Text("Promote to Coach")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            memberToRemove = member
                            memberAction = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_remove_member"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove from Team")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { memberAction = null }) { Text("Cancel") }
            }
        )
    }

    // Promote confirmation dialog
    memberToPromote?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToPromote = null },
            title = { Text("Promote to Coach") },
            text = { Text("Promote ${member.displayName} to coach?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.promoteMember(teamId, member.userId)
                        memberToPromote = null
                    },
                    modifier = Modifier.testTag("btn_promote_confirm")
                ) {
                    Text("Promote")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToPromote = null }) { Text("Cancel") }
            }
        )
    }

    // Remove confirmation dialog
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
                    modifier = Modifier.testTag("btn_remove_confirm"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { memberToRemove = null },
                    modifier = Modifier.testTag("btn_remove_cancel")
                ) {
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
                        modifier = Modifier.fillMaxWidth().testTag("btn_invite_as_player")
                    ) {
                        Text("Invite as Player")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.createCoachInvite(teamId)
                            showInviteDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_invite_as_coach")
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

    // Invite URL Dialog
    state.inviteUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { viewModel.resetInvite() },
            title = { Text("Invite Link Created") },
            text = {
                Column {
                    Text("Share this link to invite someone to the team:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(url))
                        viewModel.resetInvite()
                    },
                    modifier = Modifier.testTag("btn_copy_invite_link")
                ) {
                    Text("Copy Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetInvite() }) { Text("Close") }
            }
        )
    }

    // Edit Team Sheet
    if (state.showEditTeamSheet) {
        TeamEditSheet(
            initialName = state.teamName,
            initialDescription = state.teamDescription ?: "",
            isCreate = false,
            onSave = { name, description -> viewModel.editTeam(teamId, name, description) },
            onDismiss = { viewModel.hideEditTeamSheet() }
        )
    }

    // Sub-group Sheet
    if (state.showSubGroupSheet) {
        SubGroupSheet(
            teamId = teamId,
            subGroups = state.subGroups,
            isCoachOrManager = state.isClubManager,
            onDismiss = { viewModel.toggleSubGroupSheet() },
            onCreateSubGroup = { name -> viewModel.createSubGroup(teamId, name) },
            onDeleteSubGroup = { subGroupId -> viewModel.deleteSubGroup(teamId, subGroupId) }
        )
    }
}

@Composable
fun MemberItem(
    member: TeamMember,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_item_${member.userId}")
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
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
