package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
    palette: ThemePalette = ThemePalette.EMERALD,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= 31 -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = EmeraldPrimary,
            onPrimary = EmeraldOnPrimary,
            primaryContainer = EmeraldPrimaryContainer,
            onPrimaryContainer = EmeraldOnPrimaryContainer,
            secondary = RoseSecondary,
            onSecondary = RoseOnSecondary,
            background = SurfaceDark,
            surface = SurfaceDark
        )
        else -> lightColorScheme(
            primary = EmeraldPrimary,
            onPrimary = EmeraldOnPrimary,
            primaryContainer = EmeraldPrimaryContainer,
            onPrimaryContainer = EmeraldOnPrimaryContainer,
            secondary = RoseSecondary,
            onSecondary = RoseOnSecondary,
            background = BackgroundLight,
            surface = SurfaceLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
