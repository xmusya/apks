package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Monet harmonious base schemes (used when Android 12+ dynamic color is unavailable or as baseline)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004786),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3E4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    tertiary = Color(0xFFDCBCE1),
    onTertiary = Color(0xFF3E2845),
    tertiaryContainer = Color(0xFF563E5D),
    onTertiaryContainer = Color(0xFFF9D8FE),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    error = RadiantRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005FAF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF6F5575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF9D8FE),
    onTertiaryContainer = Color(0xFF28132F),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7D0),
    error = RadiantRed,
    onError = Color.White
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun TransmifyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true, // Monet Dynamic Color enabled by default on Android 12+
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
