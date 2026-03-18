package ch.teamorg.ui.emptystate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EmptyStateScreen(
    viewModel: EmptyStateViewModel,
    onNavigateToClubSetup: () -> Unit,
    onNavigateToInvite: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmptyStateEvent.NavigateToClubSetup -> onNavigateToClubSetup()
                is EmptyStateEvent.NavigateToInvite -> onNavigateToInvite(event.token)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090912))
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. Welcome Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Welcome to Teamorg",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = "You're not part of a team yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // 2. Join a team Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color(0xFF4F8EF7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join a team", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text("Got an invite link from your coach?", color = Color.Gray)
                    OutlinedTextField(
                        value = state.inviteLink,
                        onValueChange = { viewModel.onInviteLinkChange(it) },
                        label = { Text("Paste invite link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.onJoinTeamClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F8EF7))
                    ) {
                        Text("Join Team")
                    }
                }
            }

            // 3. Create a club Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sports, contentDescription = null, tint = Color(0xFFF97316))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create a club", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text("Starting a new club for your organization?", color = Color.Gray)
                    Button(
                        onClick = { viewModel.onCreateClubClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                    ) {
                        Text("Set up your club")
                    }
                }
            }

            // 4. Share your profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Let a coach add you", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Share your profile link so a coach can find you:", color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.profileLink,
                            modifier = Modifier.weight(1f).padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                        IconButton(onClick = { viewModel.onProfileLinkCopied() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF4F8EF7))
                        }
                    }
                }
            }
        }

        // Messages
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                action = { TextButton(onClick = { viewModel.dismissMessages() }) { Text("Dismiss") } }
            ) { Text(error) }
        }

        state.infoMessage?.let { info ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                action = { TextButton(onClick = { viewModel.dismissMessages() }) { Text("OK") } }
            ) { Text(info) }
        }
    }
}
