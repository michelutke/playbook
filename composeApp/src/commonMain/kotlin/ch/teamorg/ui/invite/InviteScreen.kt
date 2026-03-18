package ch.teamorg.ui.invite

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    token: String,
    viewModel: InviteViewModel,
    isLoggedIn: Boolean,
    onNavigateToLogin: (String) -> Unit,
    onNavigateToRegister: (String) -> Unit,
    onJoinSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(token) {
        viewModel.loadInvite(token)
    }

    LaunchedEffect(state.isRedeemed) {
        if (state.isRedeemed) {
            onJoinSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Team Invitation") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadInvite(token) }) {
                        Text("Retry")
                    }
                }
            } else if (state.inviteDetails != null) {
                val invite = state.inviteDetails!!

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "You've been invited to join",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        invite.teamName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "at ${invite.clubName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "Invited by ${invite.invitedBy} as ${invite.role.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    if (isLoggedIn) {
                        Button(
                            onClick = { viewModel.redeemInvite(token) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isRedeeming,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (state.isRedeeming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Join ${invite.teamName}")
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onNavigateToRegister(token) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Create Account to Join")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { onNavigateToLogin(token) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Login to Join")
                            }
                        }
                    }
                }
            }
        }
    }
}
