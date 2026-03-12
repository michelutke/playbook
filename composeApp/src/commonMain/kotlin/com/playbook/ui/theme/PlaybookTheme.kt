package com.playbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun PlaybookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PlaybookDarkColorScheme else PlaybookLightColorScheme
    val extendedColors = if (darkTheme) PlaybookDarkExtendedColors else PlaybookLightExtendedColors

    CompositionLocalProvider(LocalPlaybookExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlaybookTypography,
            shapes = PlaybookShapes,
            content = content
        )
    }
}

// Convenience accessor for extended colors
val MaterialTheme.extendedColors: PlaybookExtendedColors
    @Composable get() = LocalPlaybookExtendedColors.current
