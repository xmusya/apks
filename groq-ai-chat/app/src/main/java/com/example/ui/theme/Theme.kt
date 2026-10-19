package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val GroqDarkColorScheme = darkColorScheme(
    primary = GroqOrangePrimaryDark,
    onPrimary = GroqOrangeOnPrimaryDark,
    primaryContainer = GroqOrangePrimaryContainerDark,
    onPrimaryContainer = GroqOrangeOnPrimaryContainerDark,
    secondary = GroqVioletSecondaryDark,
    onSecondary = GroqVioletOnSecondaryDark,
    secondaryContainer = GroqVioletSecondaryContainerDark,
    onSecondaryContainer = GroqVioletOnSecondaryContainerDark,
    tertiary = GroqAmberTertiaryDark,
    onTertiary = GroqAmberOnTertiaryDark,
    tertiaryContainer = GroqAmberTertiaryContainerDark,
    onTertiaryContainer = GroqAmberOnTertiaryContainerDark,
    background = GroqBackgroundDark,
    onBackground = GroqOnBackgroundDark,
    surface = GroqSurfaceDark,
    onSurface = GroqOnSurfaceDark,
    surfaceVariant = GroqSurfaceVariantDark,
    onSurfaceVariant = GroqOnSurfaceVariantDark,
    outline = GroqOutlineDark,
    outlineVariant = GroqOutlineVariantDark,
    error = GroqError,
    onError = GroqOnError,
    errorContainer = GroqErrorContainer,
    onErrorContainer = GroqOnErrorContainer
)

private val GroqLightColorScheme = lightColorScheme(
    primary = GroqOrangePrimary,
    onPrimary = GroqOrangeOnPrimary,
    primaryContainer = GroqOrangePrimaryContainer,
    onPrimaryContainer = GroqOrangeOnPrimaryContainer,
    secondary = GroqVioletSecondary,
    onSecondary = GroqVioletOnSecondary,
    secondaryContainer = GroqVioletSecondaryContainer,
    onSecondaryContainer = GroqVioletOnSecondaryContainer,
    tertiary = GroqAmberTertiary,
    onTertiary = GroqAmberOnTertiary,
    tertiaryContainer = GroqAmberTertiaryContainer,
    onTertiaryContainer = GroqAmberOnTertiaryContainer,
    background = GroqBackgroundLight,
    onBackground = GroqOnBackgroundLight,
    surface = GroqSurfaceLight,
    onSurface = GroqOnSurfaceLight,
    surfaceVariant = GroqSurfaceVariantLight,
    onSurfaceVariant = GroqOnSurfaceVariantLight,
    outline = GroqOutlineLight,
    outlineVariant = GroqOutlineVariantLight,
    error = GroqError,
    onError = GroqOnError,
    errorContainer = GroqErrorContainer,
    onErrorContainer = GroqOnErrorContainer
)

@Composable
fun GroqChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GroqDarkColorScheme
        else -> GroqLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
