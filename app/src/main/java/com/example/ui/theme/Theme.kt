package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM("SYSTEM", "Ikuti Sistem"),
    LIGHT("LIGHT", "Mode Terang"),
    DARK("DARK", "Mode Gelap");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class ThemePalette(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val containerColor: Color,
    val accentColor: Color
) {
    EMERALD(
        id = "EMERALD",
        displayName = "Hijau Zamzam",
        description = "Sage alami & ketenangan sunnah",
        primaryColor = EmeraldPrimary,
        containerColor = EmeraldPrimaryContainer,
        accentColor = GoldTertiary
    ),
    ROSE(
        id = "ROSE",
        displayName = "Mawar Wardah",
        description = "Blush lembut & kenyamanan wanita",
        primaryColor = RoseWardahPrimary,
        containerColor = RoseWardahPrimaryContainer,
        accentColor = RoseWardahSecondary
    ),
    LAVENDER(
        id = "LAVENDER",
        displayName = "Lavender Sakinah",
        description = "Ungu lembut pereda stres & damai",
        primaryColor = LavenderSukoonPrimary,
        containerColor = LavenderSukoonPrimaryContainer,
        accentColor = LavenderSukoonTertiary
    ),
    OCEAN(
        id = "OCEAN",
        displayName = "Biru Samudra",
        description = "Jernih bagai kesucian air wudhu",
        primaryColor = OceanBahrPrimary,
        containerColor = OceanBahrPrimaryContainer,
        accentColor = OceanBahrTertiary
    ),
    OLIVE(
        id = "OLIVE",
        displayName = "Zaitun Barakah",
        description = "Nuansa herbal & kehangatan alami",
        primaryColor = OliveZaitunPrimary,
        containerColor = OliveZaitunPrimaryContainer,
        accentColor = OliveZaitunTertiary
    ),
    DYNAMIC(
        id = "DYNAMIC",
        displayName = "Material You (Dinamis)",
        description = "Warna otomatis dari wallpaper HP (Android 12+)",
        primaryColor = Color(0xFF006C50),
        containerColor = Color(0xFF8CF8CF),
        accentColor = Color(0xFF4C6358)
    );

    companion object {
        fun fromId(id: String?): ThemePalette {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EMERALD
        }
    }
}

// 1. Zamzam Emerald Schemes
private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = RoseSecondary,
    onSecondary = RoseOnSecondary,
    secondaryContainer = RoseSecondaryContainer,
    onSecondaryContainer = RoseOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    secondary = RoseDarkSecondary,
    onSecondary = RoseDarkOnSecondary,
    secondaryContainer = RoseDarkSecondaryContainer,
    onSecondaryContainer = RoseDarkOnSecondaryContainer,
    tertiary = GoldDarkTertiary,
    onTertiary = GoldDarkOnTertiary,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

// 2. Rose Wardah Schemes
private val RoseWardahLightScheme = lightColorScheme(
    primary = RoseWardahPrimary,
    onPrimary = RoseWardahOnPrimary,
    primaryContainer = RoseWardahPrimaryContainer,
    onPrimaryContainer = RoseWardahOnPrimaryContainer,
    secondary = RoseWardahSecondary,
    onSecondary = RoseWardahOnSecondary,
    secondaryContainer = RoseWardahSecondaryContainer,
    onSecondaryContainer = RoseWardahOnSecondaryContainer,
    tertiary = RoseWardahTertiary,
    onTertiary = RoseWardahOnTertiary,
    tertiaryContainer = RoseWardahTertiaryContainer,
    onTertiaryContainer = RoseWardahOnTertiaryContainer,
    background = RoseWardahBackgroundLight,
    onBackground = RoseWardahOnBackgroundLight,
    surface = RoseWardahSurfaceLight,
    onSurface = RoseWardahOnSurfaceLight,
    surfaceVariant = RoseWardahSurfaceVariantLight,
    onSurfaceVariant = RoseWardahOnSurfaceVariantLight,
    outline = Color(0xFF857376)
)

private val RoseWardahDarkScheme = darkColorScheme(
    primary = RoseWardahDarkPrimary,
    onPrimary = RoseWardahDarkOnPrimary,
    primaryContainer = RoseWardahDarkPrimaryContainer,
    onPrimaryContainer = RoseWardahDarkOnPrimaryContainer,
    secondary = RoseWardahDarkSecondary,
    onSecondary = RoseWardahDarkOnSecondary,
    secondaryContainer = RoseWardahDarkSecondaryContainer,
    onSecondaryContainer = RoseWardahDarkOnSecondaryContainer,
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF48290D),
    background = RoseWardahDarkBackground,
    onBackground = RoseWardahDarkOnBackground,
    surface = RoseWardahDarkSurface,
    onSurface = RoseWardahDarkOnSurface,
    surfaceVariant = Color(0xFF524346),
    onSurfaceVariant = Color(0xFFD6C2C6)
)

// 3. Sukoon Lavender Schemes
private val LavenderSukoonLightScheme = lightColorScheme(
    primary = LavenderSukoonPrimary,
    onPrimary = LavenderSukoonOnPrimary,
    primaryContainer = LavenderSukoonPrimaryContainer,
    onPrimaryContainer = LavenderSukoonOnPrimaryContainer,
    secondary = LavenderSukoonSecondary,
    onSecondary = LavenderSukoonOnSecondary,
    secondaryContainer = LavenderSukoonSecondaryContainer,
    onSecondaryContainer = LavenderSukoonOnSecondaryContainer,
    tertiary = LavenderSukoonTertiary,
    onTertiary = LavenderSukoonOnTertiary,
    tertiaryContainer = LavenderSukoonTertiaryContainer,
    onTertiaryContainer = LavenderSukoonOnTertiaryContainer,
    background = LavenderSukoonBackgroundLight,
    onBackground = LavenderSukoonOnBackgroundLight,
    surface = LavenderSukoonSurfaceLight,
    onSurface = LavenderSukoonOnSurfaceLight,
    surfaceVariant = LavenderSukoonSurfaceVariantLight,
    onSurfaceVariant = LavenderSukoonOnSurfaceVariantLight,
    outline = Color(0xFF79757F)
)

private val LavenderSukoonDarkScheme = darkColorScheme(
    primary = LavenderSukoonDarkPrimary,
    onPrimary = LavenderSukoonDarkOnPrimary,
    primaryContainer = LavenderSukoonDarkPrimaryContainer,
    onPrimaryContainer = LavenderSukoonDarkOnPrimaryContainer,
    secondary = LavenderSukoonDarkSecondary,
    onSecondary = LavenderSukoonDarkOnSecondary,
    secondaryContainer = LavenderSukoonDarkSecondaryContainer,
    onSecondaryContainer = LavenderSukoonDarkOnSecondaryContainer,
    tertiary = Color(0xFFEEB8D0),
    onTertiary = Color(0xFF4A2539),
    background = LavenderSukoonDarkBackground,
    onBackground = LavenderSukoonDarkOnBackground,
    surface = LavenderSukoonDarkSurface,
    onSurface = LavenderSukoonDarkOnSurface,
    surfaceVariant = Color(0xFF48454F),
    onSurfaceVariant = Color(0xFFC9C4D0)
)

// 4. Al-Bahr Ocean Schemes
private val OceanBahrLightScheme = lightColorScheme(
    primary = OceanBahrPrimary,
    onPrimary = OceanBahrOnPrimary,
    primaryContainer = OceanBahrPrimaryContainer,
    onPrimaryContainer = OceanBahrOnPrimaryContainer,
    secondary = OceanBahrSecondary,
    onSecondary = OceanBahrOnSecondary,
    secondaryContainer = OceanBahrSecondaryContainer,
    onSecondaryContainer = OceanBahrOnSecondaryContainer,
    tertiary = OceanBahrTertiary,
    onTertiary = OceanBahrOnTertiary,
    tertiaryContainer = OceanBahrTertiaryContainer,
    onTertiaryContainer = OceanBahrOnTertiaryContainer,
    background = OceanBahrBackgroundLight,
    onBackground = OceanBahrOnBackgroundLight,
    surface = OceanBahrSurfaceLight,
    onSurface = OceanBahrOnSurfaceLight,
    surfaceVariant = OceanBahrSurfaceVariantLight,
    onSurfaceVariant = OceanBahrOnSurfaceVariantLight,
    outline = Color(0xFF72787E)
)

private val OceanBahrDarkScheme = darkColorScheme(
    primary = OceanBahrDarkPrimary,
    onPrimary = OceanBahrDarkOnPrimary,
    primaryContainer = OceanBahrDarkPrimaryContainer,
    onPrimaryContainer = OceanBahrDarkOnPrimaryContainer,
    secondary = OceanBahrDarkSecondary,
    onSecondary = OceanBahrDarkOnSecondary,
    secondaryContainer = OceanBahrDarkSecondaryContainer,
    onSecondaryContainer = OceanBahrDarkOnSecondaryContainer,
    tertiary = Color(0xFF96CCDF),
    onTertiary = Color(0xFF003644),
    background = OceanBahrDarkBackground,
    onBackground = OceanBahrDarkOnBackground,
    surface = OceanBahrDarkSurface,
    onSurface = OceanBahrDarkOnSurface,
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE)
)

// 5. Zaitun Amber Schemes
private val OliveZaitunLightScheme = lightColorScheme(
    primary = OliveZaitunPrimary,
    onPrimary = OliveZaitunOnPrimary,
    primaryContainer = OliveZaitunPrimaryContainer,
    onPrimaryContainer = OliveZaitunOnPrimaryContainer,
    secondary = OliveZaitunSecondary,
    onSecondary = OliveZaitunOnSecondary,
    secondaryContainer = OliveZaitunSecondaryContainer,
    onSecondaryContainer = OliveZaitunOnSecondaryContainer,
    tertiary = OliveZaitunTertiary,
    onTertiary = OliveZaitunOnTertiary,
    tertiaryContainer = OliveZaitunTertiaryContainer,
    onTertiaryContainer = OliveZaitunOnTertiaryContainer,
    background = OliveZaitunBackgroundLight,
    onBackground = OliveZaitunOnBackgroundLight,
    surface = OliveZaitunSurfaceLight,
    onSurface = OliveZaitunOnSurfaceLight,
    surfaceVariant = OliveZaitunSurfaceVariantLight,
    onSurfaceVariant = OliveZaitunOnSurfaceVariantLight,
    outline = Color(0xFF76786B)
)

private val OliveZaitunDarkScheme = darkColorScheme(
    primary = OliveZaitunDarkPrimary,
    onPrimary = OliveZaitunDarkOnPrimary,
    primaryContainer = OliveZaitunDarkPrimaryContainer,
    onPrimaryContainer = OliveZaitunDarkOnPrimaryContainer,
    secondary = OliveZaitunDarkSecondary,
    onSecondary = OliveZaitunDarkOnSecondary,
    secondaryContainer = OliveZaitunDarkSecondaryContainer,
    onSecondaryContainer = OliveZaitunDarkOnSecondaryContainer,
    tertiary = Color(0xFFFBB872),
    onTertiary = Color(0xFF4A2800),
    background = OliveZaitunDarkBackground,
    onBackground = OliveZaitunDarkOnBackground,
    surface = OliveZaitunDarkSurface,
    onSurface = OliveZaitunDarkOnSurface,
    surfaceVariant = Color(0xFF46483C),
    onSurfaceVariant = Color(0xFFC7C8B8)
)

@Composable
fun MyApplicationTheme(
    palette: ThemePalette = ThemePalette.EMERALD,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == ThemePalette.ROSE -> if (isDark) RoseWardahDarkScheme else RoseWardahLightScheme
        palette == ThemePalette.LAVENDER -> if (isDark) LavenderSukoonDarkScheme else LavenderSukoonLightScheme
        palette == ThemePalette.OCEAN -> if (isDark) OceanBahrDarkScheme else OceanBahrLightScheme
        palette == ThemePalette.OLIVE -> if (isDark) OliveZaitunDarkScheme else OliveZaitunLightScheme
        else -> if (isDark) EmeraldDarkScheme else EmeraldLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

