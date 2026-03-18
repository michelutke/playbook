package ch.teamorg.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun TeamorgTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TeamorgDarkColorScheme else TeamorgLightColorScheme
    val extendedColors = if (darkTheme) TeamorgDarkExtendedColors else TeamorgLightExtendedColors

    CompositionLocalProvider(LocalTeamorgExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TeamorgTypography,
            shapes = TeamorgShapes,
            content = content
        )
    }
}

// Convenience accessor for extended colors
val MaterialTheme.extendedColors: TeamorgExtendedColors
    @Composable get() = LocalTeamorgExtendedColors.current
