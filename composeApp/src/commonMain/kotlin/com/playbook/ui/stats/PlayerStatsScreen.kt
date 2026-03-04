package com.playbook.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.domain.AttendanceStats
import com.playbook.domain.EventType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    userId: String,
    teamId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PlayerStatsViewModel = koinViewModel { parametersOf(userId, teamId) },
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
            state.error != null -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = state.selectedEventType == null,
                        onClick = { viewModel.submitAction(PlayerStatsAction.FilterByEventType(null)) },
                        label = { Text("All") },
                    )
                    FilterChip(
                        selected = state.selectedEventType == EventType.TRAINING,
                        onClick = { viewModel.submitAction(PlayerStatsAction.FilterByEventType(EventType.TRAINING)) },
                        label = { Text("Training") },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    FilterChip(
                        selected = state.selectedEventType == EventType.MATCH,
                        onClick = { viewModel.submitAction(PlayerStatsAction.FilterByEventType(EventType.MATCH)) },
                        label = { Text("Match") },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (state.stats != null) {
                    StatsCard(stats = state.stats!!)
                } else {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(stats: AttendanceStats, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow(label = "Overall", pct = stats.presencePct, total = stats.totalEvents)
            Spacer(Modifier.height(12.dp))
            StatRow(label = "Training", pct = stats.trainingPct, total = stats.totalTraining)
            Spacer(Modifier.height(12.dp))
            StatRow(label = "Match", pct = stats.matchPct, total = stats.totalMatches)
        }
    }
}

@Composable
private fun StatRow(label: String, pct: Double, total: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "$total events",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
