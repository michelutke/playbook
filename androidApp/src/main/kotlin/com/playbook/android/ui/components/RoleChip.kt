package com.playbook.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playbook.domain.MemberRole

enum class UiRole { CLUB_MANAGER, COACH, PLAYER }

fun MemberRole.toUiRole(): UiRole = when (this) {
    MemberRole.COACH -> UiRole.COACH
    MemberRole.PLAYER -> UiRole.PLAYER
}

@Composable
fun RoleChip(role: UiRole, modifier: Modifier = Modifier) {
    val backgroundColor = when (role) {
        UiRole.CLUB_MANAGER -> Color(0xFFFFC107)
        UiRole.COACH -> MaterialTheme.colorScheme.primary
        UiRole.PLAYER -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (role) {
        UiRole.CLUB_MANAGER -> Color(0xFF1A1A1A)
        UiRole.COACH -> MaterialTheme.colorScheme.onPrimary
        UiRole.PLAYER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (role) {
        UiRole.CLUB_MANAGER -> "Manager"
        UiRole.COACH -> "Coach"
        UiRole.PLAYER -> "Player"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
