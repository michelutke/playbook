package com.playbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playbook.domain.EventType

val EventType.color: Color
    get() = when (this) {
        EventType.TRAINING -> Color(0xFF1E88E5)
        EventType.MATCH -> Color(0xFF43A047)
        EventType.OTHER -> Color(0xFF8E24AA)
    }

val EventType.label: String
    get() = when (this) {
        EventType.TRAINING -> "Training"
        EventType.MATCH -> "Match"
        EventType.OTHER -> "Other"
    }

val EventType.icon: String
    get() = when (this) {
        EventType.TRAINING -> "🏃"
        EventType.MATCH -> "⚽"
        EventType.OTHER -> "📅"
    }

@Composable
fun EventTypeDot(type: EventType, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(8.dp).background(type.color, CircleShape))
}

@Composable
fun EventTypeChip(type: EventType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(type.color.copy(alpha = 0.12f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${type.icon} ${type.label}",
            style = MaterialTheme.typography.labelSmall,
            color = type.color,
        )
    }
}
