package com.playbook.ui.playerprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.playbook.domain.MemberRole
import com.playbook.ui.components.RoleChip
import com.playbook.ui.components.UiRole
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlayerProfileScreen(
    teamId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerProfileViewModel = koinViewModel { parametersOf(teamId, userId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlayerProfileEvent.NavigateBack -> onNavigateBack()
            }
        }
    }
    PlayerProfileContent(
        state = state,
        onAction = viewModel::submitAction,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProfileContent(
    state: PlayerProfileScreenState,
    onAction: (PlayerProfileAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val member = state.member
    val isCoach = member?.roles?.contains(MemberRole.COACH) == true
    val isPlayerOnly = member?.roles?.contains(MemberRole.PLAYER) == true && !isCoach

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(member?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (isPlayerOnly) {
                            DropdownMenuItem(
                                text = { Text("Add Coach Role") },
                                onClick = {
                                    menuExpanded = false
                                    onAction(PlayerProfileAction.AddCoachRole)
                                },
                            )
                        }
                        if (isCoach) {
                            DropdownMenuItem(
                                text = { Text("Remove Coach Role") },
                                onClick = {
                                    menuExpanded = false
                                    onAction(PlayerProfileAction.RemoveCoachRole)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Team", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onAction(PlayerProfileAction.RemoveFromTeam)
                            },
                        )
                    }
                },
            )
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    AsyncImage(
                        model = member?.avatarUrl,
                        contentDescription = member?.displayName,
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        member?.displayName ?: member?.userId ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        member?.roles?.forEach { role ->
                            RoleChip(
                                role = when (role) {
                                    MemberRole.COACH -> UiRole.COACH
                                    MemberRole.PLAYER -> UiRole.PLAYER
                                },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Team Info", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (state.profile?.jerseyNumber != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Jersey",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "#${state.profile.jerseyNumber}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                            val profilePosition = state.profile?.position
                            if (profilePosition != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Position",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        profilePosition,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                            if (state.profile?.jerseyNumber == null && state.profile?.position == null) {
                                Text(
                                    "No team info set.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Attendance", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Attendance stats coming soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
