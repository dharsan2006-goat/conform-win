package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// MeshWave theme — Dark + Electric Blue
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),        // Electric blue
    onPrimary = Color.Black,
    secondary = Color(0xFF0066CC),      // Deep blue
    onSecondary = Color.White,
    background = Color(0xFF0D1117),     // GitHub-dark background
    onBackground = Color(0xFF0A84FF),   // Blue on dark
    surface = Color(0xFF161B22),        // Dark surface
    onSurface = Color(0xFF0A84FF),      // Blue text
    error = Color(0xFFFF5555),          // Red for errors
    onError = Color.Black,
    tertiary = Color(0x99EBEBF5),
    primaryContainer = Color(0xFF1A3A5C),  // Muted blue container
    onPrimaryContainer = Color(0xFF80BFFF), // Light blue on container
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),        // Deep blue
    onPrimary = Color.White,
    secondary = Color(0xFF004999),      // Even deeper blue
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF0066CC),   // Blue on white
    surface = Color(0xFFF0F6FF),        // Very light blue tint
    onSurface = Color(0xFF0066CC),      // Blue text
    error = Color(0xFFCC0000),          // Dark red for errors
    onError = Color.White,
    tertiary = Color(0x993C3C43),
    primaryContainer = Color(0xFFDCEBFF),  // Light blue container
    onPrimaryContainer = Color(0xFF003366), // Dark blue on container
)

@Composable
fun MeshWaveTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.System)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
