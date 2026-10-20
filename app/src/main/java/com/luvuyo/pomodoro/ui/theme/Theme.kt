package com.luvuyo.pomodoro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TomatoLight,
    onPrimary = OnTomatoLight,
    primaryContainer = TomatoContainerLight,
    onPrimaryContainer = OnTomatoContainerLight,
    secondary = BlueLight,
    onSecondary = OnBlueLight,
    secondaryContainer = BlueContainerLight,
    onSecondaryContainer = OnBlueContainerLight,
    tertiary = GreenLight,
    onTertiary = OnGreenLight,
    tertiaryContainer = GreenContainerLight,
    onTertiaryContainer = OnGreenContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColors = darkColorScheme(
    primary = TomatoDark,
    onPrimary = OnTomatoDark,
    primaryContainer = TomatoContainerDark,
    onPrimaryContainer = OnTomatoContainerDark,
    secondary = BlueDark,
    onSecondary = OnBlueDark,
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = OnBlueContainerDark,
    tertiary = GreenDark,
    onTertiary = OnGreenDark,
    tertiaryContainer = GreenContainerDark,
    onTertiaryContainer = OnGreenContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

/**
 * App theme. Follows the system light/dark setting. Dynamic colour is deliberately not used,
 * so Work/Short Break/Long Break keep their recognisable colours on every device.
 */
@Composable
fun PomodoroTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, // from the generated Type.kt
        content = content
    )
}