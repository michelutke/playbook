package com.playbook.ui.teamdetail

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.playbook.domain.MemberRole
import com.playbook.domain.RosterMember
import com.playbook.ui.components.RoleChip
import com.playbook.ui.components.UiRole
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TeamDetailScreen(
    teamId: String,
    clubId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEvents: () -> Unit = {},
    onNavigateToSubgroups: () -> Unit = {},
    onNavigateToEditTeam: () -> Unit = {},
    onNavigateToInvite: () -> Unit = {},
    onNavigateToPlayerProfile: (String) -> Unit = {},
    onNavigateToTeamStats: () -> Unit = {},
    viewModel: TeamDetailViewModel = kmpViewModel(key = teamId) { parametersOf(teamId, clubId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TeamDetailEvent.NavigateBack -> onNavigateBack()
            }
        }
    }
    TeamDetailContent(
        state = state,
        onAction = viewModel::submitAction,
        onNavigateBack = onNavigateBack,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToSubgroups = onNavigateToSubgroups,
        onNavigateToEditTeam = onNavigateToEditTeam,
        onNavigateToInvite = onNavigateToInvite,
        onNavigateToPlayerProfile = onNavigateToPlayerProfile,
        onNavigateToTeamStats = onNavigateToTeamStats,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamDetailContent(
    state: TeamDetailScreenState,
    onAction: (TeamDetailAction) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSubgroups: () -> Unit,
    onNavigateToEditTeam: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToPlayerProfile: (String) -> Unit,
    onNavigateToTeamStats: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.team?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val tabs = TeamDetailTab.entries
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onAction(TeamDetailAction.TabSelected(tab)) },
                        modifier = Modifier.semantics {
                            testTag = when (tab) {
                                TeamDetailTab.ROSTER -> "roster_tab"
                                TeamDetailTab.SUB_GROUPS -> "sub_groups_tab"
                                TeamDetailTab.SETTINGS -> "settings_tab"
                            }
                        },
                        text = {
                            Text(
                                when (tab) {
                                    TeamDetailTab.ROSTER -> "Roster"
                                    TeamDetailTab.SUB_GROUPS -> "Sub-groups"
                                    TeamDetailTab.SETTINGS -> "Settings"
                                }
                            )
                        },
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                else -> when (state.selectedTab) {
                    TeamDetailTab.ROSTER -> RosterTab(state, onAction, onNavigateToPlayerProfile)
                    TeamDetailTab.SUB_GROUPS -> SubGroupsTab(onNavigateToSubgroups, onNavigateToEvents)
                    TeamDetailTab.SETTINGS -> SettingsTab(
                        state = state,
                        onAction = onAction,
                        onNavigateToEditTeam = onNavigateToEditTeam,
                        onNavigateToInvite = onNavigateToInvite,
                        onNavigateToTeamStats = onNavigateToTeamStats,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RosterTab(
    state: TeamDetailScreenState,
    onAction: (TeamDetailAction) -> Unit,
    onNavigateToPlayerProfile: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(TeamDetailAction.SearchQueryChanged(it)) },
                label = { Text("Search members") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics { testTag = "search_field" },
                singleLine = true,
            )
        }

        if (state.coaches.isNotEmpty()) {
            item {
                Text(
                    text = "Coaches",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(state.coaches, key = { "coach_${it.userId}" }) { member ->
                SwipeToDismissRosterItem(
                    member = member,
                    onDismiss = { onAction(TeamDetailAction.RemoveMember(member.userId)) },
                    onClick = { onNavigateToPlayerProfile(member.userId) },
                )
            }
        }

        if (state.players.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Players",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(state.players, key = { "player_${it.userId}" }) { member ->
                SwipeToDismissRosterItem(
                    member = member,
                    onDismiss = { onAction(TeamDetailAction.RemoveMember(member.userId)) },
                    onClick = { onNavigateToPlayerProfile(member.userId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissRosterItem(member: RosterMember, onDismiss: () -> Unit, onClick: () -> Unit = {}) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick).semantics { testTag = "member_item" },
            headlineContent = { Text(member.displayName ?: member.userId) },
            supportingContent = {
                Row {
                    member.roles.forEach { role ->
                        RoleChip(
                            role = when (role) {
                                MemberRole.COACH -> UiRole.COACH
                                MemberRole.PLAYER -> UiRole.PLAYER
                            },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            },
            leadingContent = {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = member.displayName,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            },
        )
    }
}

@Composable
private fun SubGroupsTab(onNavigateToSubgroups: () -> Unit, onNavigateToEvents: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onNavigateToSubgroups, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Sub-groups")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onNavigateToEvents, modifier = Modifier.fillMaxWidth()) {
            Text("View Events")
        }
    }
}

@Composable
private fun SettingsTab(
    state: TeamDetailScreenState,
    onAction: (TeamDetailAction) -> Unit,
    onNavigateToEditTeam: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToTeamStats: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Team", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(state.team?.name ?: "", style = MaterialTheme.typography.bodyLarge)
                    val teamDesc = state.team?.description
                    if (teamDesc != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            teamDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToEditTeam,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Edit Team")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onNavigateToInvite, modifier = Modifier.fillMaxWidth()) {
                Text("Invite Members")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToTeamStats, modifier = Modifier.fillMaxWidth()) {
                Text("Team Statistics")
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onAction(TeamDetailAction.LeaveTeam) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Leave Team")
                    }
                }
            }
        }
    }
}
