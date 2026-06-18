package com.example.musicplayer.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    tertiary = AccentSecondary,
    background = DeepBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,

    // THE FIX: Explicitly setting the container colors to override the default Purple!
    primaryContainer = Color(0xFF00333D), // A sleek, deep dark cyan for the MiniPlayer/Selected Song
    onPrimaryContainer = AccentPrimary,   // Makes the icons and text pop with Electric Cyan
    secondaryContainer = Color(0xFF2A2A2A), // Dark gray for album art placeholders
    onSecondaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimaryLight,
    secondary = AccentPrimaryLight,
    background = LightBackground,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray,

    // Light theme overrides
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = AccentPrimaryLight,
    secondaryContainer = Color(0xFFF3EDF7),
    onSecondaryContainer = Color.Black
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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