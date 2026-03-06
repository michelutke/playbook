package com.playbook.ui.inviteaccept

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.domain.MemberRole
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InviteAcceptScreen(
    token: String,
    onAccepted: (clubId: String) -> Unit,
    onDeclined: () -> Unit,
    viewModel: InviteAcceptViewModel = kmpViewModel(key = token) { parametersOf(token) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InviteAcceptEvent.Accepted -> onAccepted(event.clubId)
                is InviteAcceptEvent.Declined -> onDeclined()
            }
        }
    }
    InviteAcceptContent(state = state, onAction = viewModel::submitAction)
}

@Composable
private fun InviteAcceptContent(
    state: InviteAcceptScreenState,
    onAction: (InviteAcceptAction) -> Unit,
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.context == null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "This link has expired. Ask your coach for a new one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val invite = state.context.invite
                    val roleLabel = when (invite.role) {
                        MemberRole.COACH -> "Coach"
                        MemberRole.PLAYER -> "Player"
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Join ${state.context.teamName} as $roleLabel?",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                state.context.clubName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (state.error != null) {
                                Text(
                                    text = state.error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { onAction(InviteAcceptAction.Accept) },
                                enabled = !state.isAccepting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (state.isAccepting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text("Join Team")
                                }
                            }

                            TextButton(
                                onClick = { onAction(InviteAcceptAction.Decline) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }
        }
    }
}
