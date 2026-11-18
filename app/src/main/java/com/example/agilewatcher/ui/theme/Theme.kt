package com.example.agilewatcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class AgileThemeColors(
    val isDark: Boolean,
    val pageBackground: Color,
    val cardSurface: Color,
    val segmentTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val gridLine: Color,
    val bottomBarPill: Color
)

val LocalAgileColors = staticCompositionLocalOf {
    AgileThemeColors(
        isDark = false,
        pageBackground = PageBackgroundLight,
        cardSurface = CardSurfaceLight,
        segmentTrack = SegmentTrackLight,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        textMuted = TextMutedLight,
        gridLine = GridLineLight,
        bottomBarPill = BottomBarPillLight
    )
}

private val LightScheme = lightColorScheme(
    primary = AgilePinkPrimary,
    secondary = AgilePinkSoftPill,
    background = PageBackgroundLight,
    surface = CardSurfaceLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

private val DarkScheme = darkColorScheme(
    primary = AgilePinkOnboardingBtn,
    secondary = AgilePinkSoftPill,
    background = NavyBackgroundDark,
    surface = NavyCardDark,
    onPrimary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

@Composable
fun AgileWatcherTheme(
    themeMode: String = "LIGHT",
    forceDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = forceDark || when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    val agileColors = if (useDark) {
        AgileThemeColors(
            isDark = true,
            pageBackground = NavyBackgroundDark,
            cardSurface = NavyCardDark,
            segmentTrack = NavySegmentDark,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textMuted = TextMutedDark,
            gridLine = GridLineDark,
            bottomBarPill = NavySegmentDark
        )
    } else {
        AgileThemeColors(
            isDark = false,
            pageBackground = PageBackgroundLight,
            cardSurface = CardSurfaceLight,
            segmentTrack = SegmentTrackLight,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textMuted = TextMutedLight,
            gridLine = GridLineLight,
            bottomBarPill = BottomBarPillLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = agileColors.pageBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    CompositionLocalProvider(LocalAgileColors provides agileColors) {
        MaterialTheme(
            colorScheme = if (useDark) DarkScheme else LightScheme,
            typography = Typography,
            content = content
        )
    }
}
