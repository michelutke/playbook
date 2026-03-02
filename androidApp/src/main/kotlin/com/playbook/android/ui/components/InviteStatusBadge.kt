package com.playbook.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playbook.domain.InviteStatus

@Composable
fun InviteStatusBadge(status: InviteStatus, modifier: Modifier = Modifier) {
    val (icon, label, color) = when (status) {
        InviteStatus.PENDING -> Triple(Icons.Outlined.Schedule, "Awaiting", Color(0xFFFF9800))
        InviteStatus.ACCEPTED -> Triple(Icons.Outlined.CheckCircle, "Accepted", Color(0xFF4CAF50))
        InviteStatus.EXPIRED -> Triple(Icons.Outlined.Cancel, "Expired", MaterialTheme.colorScheme.onSurfaceVariant)
        InviteStatus.REVOKED -> Triple(Icons.Outlined.Block, "Revoked", MaterialTheme.colorScheme.error)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}
