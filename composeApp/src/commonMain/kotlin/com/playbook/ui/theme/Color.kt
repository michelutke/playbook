package com.playbook.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark Colors (Design Tokens)
val BackgroundDark = Color(0xFF090912)
val SurfaceDark = Color(0xFF13131F)
val CardDark = Color(0xFF1C1C2E)
val ForegroundDark = Color(0xFFF0F0FF)
val MutedForegroundDark = Color(0xFF9090B0)
val BorderDark = Color(0xFF2A2A40)
val PrimaryBlue = Color(0xFF4F8EF7)
val PrimaryForeground = Color(0xFFFFFFFF)
val AccentOrange = Color(0xFFF97316)
val DestructiveRed = Color(0xFFEF4444)

// Status Colors
val SuccessGreen = Color(0xFF22C55E)
val WarningYellow = Color(0xFFFACC15)
val ColorPurple = Color(0xFFA855F7)
val ColorGold = Color(0xFFFACC15)
val ColorGrey = Color(0xFF6B7280)

val PlaybookDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = PrimaryForeground,
    secondary = AccentOrange,
    onSecondary = PrimaryForeground,
    error = DestructiveRed,
    onError = PrimaryForeground,
    background = BackgroundDark,
    onBackground = ForegroundDark,
    surface = SurfaceDark,
    onSurface = ForegroundDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = MutedForegroundDark,
    outline = BorderDark
)

val PlaybookLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = PrimaryForeground,
    secondary = AccentOrange,
    onSecondary = PrimaryForeground,
    error = DestructiveRed,
    onError = PrimaryForeground,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A1A),
    surface = Color(0xFFF4F4F9),
    onSurface = Color(0xFF0A0A1A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE2E2F0)
)

@Immutable
data class PlaybookExtendedColors(
    val success: Color,
    val warning: Color,
    val colorPurple: Color,
    val colorGold: Color,
    val colorGrey: Color,
    val cardBackground: Color,
)

val LocalPlaybookExtendedColors = staticCompositionLocalOf {
    PlaybookExtendedColors(
        success = SuccessGreen,
        warning = WarningYellow,
        colorPurple = ColorPurple,
        colorGold = ColorGold,
        colorGrey = ColorGrey,
        cardBackground = CardDark
    )
}

val PlaybookDarkExtendedColors = PlaybookExtendedColors(
    success = SuccessGreen,
    warning = WarningYellow,
    colorPurple = ColorPurple,
    colorGold = ColorGold,
    colorGrey = ColorGrey,
    cardBackground = CardDark
)

val PlaybookLightExtendedColors = PlaybookExtendedColors(
    success = SuccessGreen,
    warning = WarningYellow,
    colorPurple = ColorPurple,
    colorGold = ColorGold,
    colorGrey = ColorGrey,
    cardBackground = Color(0xFFFFFFFF)
)
