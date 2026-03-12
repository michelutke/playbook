package com.playbook.ui.teaminvite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.domain.InviteStatus
import com.playbook.domain.MemberRole
import com.playbook.ui.components.InviteStatusBadge
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamInviteSheet(
    teamId: String,
    onDismiss: () -> Unit,
    viewModel: TeamInviteViewModel = kmpViewModel(key = teamId) { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Invite to ${state.teamName}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.role == MemberRole.PLAYER,
                        onClick = { viewModel.submitAction(TeamInviteAction.RoleChanged(MemberRole.PLAYER)) },
                        label = { Text("Player") },
                    )
                    FilterChip(
                        selected = state.role == MemberRole.COACH,
                        onClick = { viewModel.submitAction(TeamInviteAction.RoleChanged(MemberRole.COACH)) },
                        label = { Text("Coach") },
                    )
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.submitAction(TeamInviteAction.EmailChanged(it)) },
                        label = { Text("Email address") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(
                        onClick = { viewModel.submitAction(TeamInviteAction.Send) },
                        enabled = state.email.isNotBlank() && !state.isSending,
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Send")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "or",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "playbook://join/$teamId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Link expires in 7 days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.submitAction(TeamInviteAction.CopyLink) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link")
                    }
                }
            }

            val inviteError = state.error
            if (inviteError != null) {
                item {
                    Text(
                        text = inviteError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (state.pendingInvites.isNotEmpty()) {
                item {
                    Text("Pending Invites", style = MaterialTheme.typography.titleSmall)
                }
                items(state.pendingInvites) { invite ->
                    ListItem(
                        headlineContent = { Text(invite.invitedEmail) },
                        supportingContent = { InviteStatusBadge(status = invite.status) },
                        trailingContent = {
                            if (invite.status == InviteStatus.PENDING) {
                                IconButton(
                                    onClick = { viewModel.submitAction(TeamInviteAction.Revoke(invite.id)) },
                                ) {
                                    Icon(
                                        Icons.Outlined.Cancel,
                                        contentDescription = "Revoke",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
