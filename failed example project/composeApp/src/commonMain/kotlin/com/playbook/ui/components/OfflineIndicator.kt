package com.playbook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OfflineQueueBadge(pendingCount: Int, modifier: Modifier = Modifier) {
    if (pendingCount > 0) {
        Box(modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape).padding(4.dp)) {
            Text("$pendingCount", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SyncingSnackbar(isSyncing: Boolean) {
    AnimatedVisibility(visible = isSyncing) {
        Snackbar { Text("Syncing…") }
    }
}
