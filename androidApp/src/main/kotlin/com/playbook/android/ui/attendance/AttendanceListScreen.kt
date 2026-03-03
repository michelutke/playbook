package com.playbook.android.ui.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playbook.android.ui.components.StatusBadge
import com.playbook.domain.AttendanceEntry
import com.playbook.domain.AttendanceResponseStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceListScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: AttendanceListViewModel = koinViewModel { parametersOf(eventId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
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
            state.teamView != null -> {
                val tv = state.teamView!!
                LazyColumn(modifier = Modifier.padding(padding)) {
                    if (tv.confirmed.isNotEmpty()) {
                        item { SectionHeader("Confirmed", tv.confirmed.size) }
                        items(tv.confirmed) { entry ->
                            AttendanceRow(
                                entry = entry,
                                isExpanded = state.expandedUserId == entry.userId,
                                onClick = { viewModel.submitAction(AttendanceListAction.ToggleExpand(entry.userId)) },
                            )
                        }
                    }
                    if (tv.unsure.isNotEmpty()) {
                        item { SectionHeader("Unsure", tv.unsure.size) }
                        items(tv.unsure) { entry ->
                            AttendanceRow(
                                entry = entry,
                                isExpanded = state.expandedUserId == entry.userId,
                                onClick = { viewModel.submitAction(AttendanceListAction.ToggleExpand(entry.userId)) },
                            )
                        }
                    }
                    if (tv.declined.isNotEmpty()) {
                        item { SectionHeader("Declined", tv.declined.size) }
                        items(tv.declined) { entry ->
                            AttendanceRow(
                                entry = entry,
                                isExpanded = state.expandedUserId == entry.userId,
                                onClick = { viewModel.submitAction(AttendanceListAction.ToggleExpand(entry.userId)) },
                            )
                        }
                    }
                    if (tv.noResponse.isNotEmpty()) {
                        item { SectionHeader("No Response", tv.noResponse.size) }
                        items(tv.noResponse) { entry ->
                            AttendanceRow(
                                entry = entry,
                                isExpanded = state.expandedUserId == entry.userId,
                                onClick = { viewModel.submitAction(AttendanceListAction.ToggleExpand(entry.userId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text("($count)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AttendanceRow(
    entry: AttendanceEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val status = entry.response?.status ?: AttendanceResponseStatus.NO_RESPONSE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    entry.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                entry.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            StatusBadge(status = status)
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 48.dp)) {
                val reason = entry.response?.reason
                if (!reason.isNullOrBlank()) {
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.response?.abwesenheitRuleId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("↻", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(4.dp))
                        Text("Auto-declined via absence rule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
