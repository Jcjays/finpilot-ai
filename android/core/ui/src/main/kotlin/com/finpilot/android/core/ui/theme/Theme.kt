package com.finpilot.android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Emerald40,
    onPrimary = Neutral99,
    primaryContainer = Emerald90,
    onPrimaryContainer = Emerald10,
    secondary = Moss40,
    onSecondary = Neutral99,
    secondaryContainer = Moss90,
    onSecondaryContainer = Moss10,
    tertiary = Gold40,
    onTertiary = Neutral99,
    tertiaryContainer = Gold90,
    onTertiaryContainer = Neutral10,
    error = Danger40,
    onError = Neutral99,
    errorContainer = Danger90,
    onErrorContainer = Neutral10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Moss30,
    outline = Moss40,
)

private val DarkColors = darkColorScheme(
    primary = Emerald80,
    onPrimary = Emerald20,
    primaryContainer = Emerald30,
    onPrimaryContainer = Emerald90,
    secondary = Moss80,
    onSecondary = Moss10,
    secondaryContainer = Moss30,
    onSecondaryContainer = Moss90,
    tertiary = Gold80,
    onTertiary = Neutral10,
    tertiaryContainer = Gold40,
    onTertiaryContainer = Gold90,
    error = Danger80,
    onError = Neutral10,
    errorContainer = Danger40,
    onErrorContainer = Danger90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Moss80,
    outline = Moss80,
)

@Composable
fun FinPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FinPilotTypography,
        content = content,
    )
}
