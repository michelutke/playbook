package com.playbook.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playbook.domain.AttendanceRecordStatus
import com.playbook.domain.AttendanceResponseStatus

@Composable
fun StatusBadge(status: AttendanceResponseStatus, modifier: Modifier = Modifier) {
    val (dotColor, borderColor, label) = when (status) {
        AttendanceResponseStatus.CONFIRMED -> Triple(Color(0xFF4CAF50), Color.Transparent, "Confirmed")
        AttendanceResponseStatus.DECLINED -> Triple(Color(0xFFF44336), Color.Transparent, "Declined")
        AttendanceResponseStatus.UNSURE -> Triple(Color(0xFFFFB300), Color.Transparent, "Unsure")
        AttendanceResponseStatus.DECLINED_AUTO -> Triple(Color(0xFF9E9E9E), Color(0xFF9E9E9E), "Auto")
        AttendanceResponseStatus.NO_RESPONSE -> Triple(Color.Transparent, Color(0xFFBDBDBD), "No response")
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val dotModifier = if (status == AttendanceResponseStatus.NO_RESPONSE) {
            Modifier.size(8.dp).clip(CircleShape).border(1.dp, borderColor, CircleShape)
        } else {
            Modifier.size(8.dp).clip(CircleShape).background(dotColor)
        }
        Box(dotModifier)
        if (status == AttendanceResponseStatus.DECLINED_AUTO) {
            Text(" ↻", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AttendanceRecordBadge(status: AttendanceRecordStatus, modifier: Modifier = Modifier) {
    val (dotColor, label) = when (status) {
        AttendanceRecordStatus.PRESENT -> Pair(Color(0xFF4CAF50), "Present")
        AttendanceRecordStatus.ABSENT -> Pair(Color(0xFFF44336), "Absent")
        AttendanceRecordStatus.EXCUSED -> Pair(Color(0xFF2196F3), "Excused")
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
