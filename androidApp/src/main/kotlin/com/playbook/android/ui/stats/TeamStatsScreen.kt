package com.playbook.android.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playbook.domain.AttendanceStats
import com.playbook.domain.EventType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamStatsScreen(
    teamId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlayerStats: (String) -> Unit,
    viewModel: TeamStatsViewModel = koinViewModel { parametersOf(teamId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FilterChip(
                            selected = state.selectedEventType == null,
                            onClick = { viewModel.submitAction(TeamStatsAction.FilterByEventType(null)) },
                            label = { Text("All") },
                        )
                        FilterChip(
                            selected = state.selectedEventType == EventType.TRAINING,
                            onClick = { viewModel.submitAction(TeamStatsAction.FilterByEventType(EventType.TRAINING)) },
                            label = { Text("Training") },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        FilterChip(
                            selected = state.selectedEventType == EventType.MATCH,
                            onClick = { viewModel.submitAction(TeamStatsAction.FilterByEventType(EventType.MATCH)) },
                            label = { Text("Match") },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                items(state.userStats) { (userId, stats) ->
                    TeamStatRow(
                        userId = userId,
                        stats = stats,
                        onClick = { onNavigateToPlayerStats(userId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamStatRow(
    userId: String,
    stats: AttendanceStats,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                userId.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(userId.take(8), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { stats.presencePct.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("${(stats.presencePct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}
