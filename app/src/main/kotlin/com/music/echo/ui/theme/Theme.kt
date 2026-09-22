package iad1tya.echo.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun isAppInDarkTheme(): Boolean {
    return LocalDarkTheme.current
}

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun echomusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor, 
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot 
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        val base = if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
        base.applyGlassmorphism(darkTheme)
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography, 
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

/**
 * Ultra-Premium Glassmorphism / Neomorphism Color Overrides.
 *
 * LIGHT MODE PHILOSOPHY (Neumorphism 2.0 — "Ivory Porcelain"):
 * - Background canvas: Warm off-white (#F5F7FA) not cold grey
 * - All surfaces: Pure white (#FFFFFF) for maximum lifted feel
 * - Shadows: Ultra-soft, low-opacity blue-grey — never dark/muddy
 * - Cards should feel like they FLOAT above the background
 * - Text: Rich slate (#1A202C) for luxury contrast
 *
 * DARK MODE: Obsidian Titanium glassmorphism (unchanged).
 */
fun ColorScheme.applyGlassmorphism(isDark: Boolean) = copy(
    // Canvas / Page Background
    background = if (isDark) Color(0xFF090A0F) else Color(0xFFF5F7FA),
    onBackground = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1A202C),

    // Primary Surface (cards, dialogs, sheets)
    surface = if (isDark) Color(0xFF10121A).copy(alpha = 0.85f) else Color(0xFFFFFFFF),
    onSurface = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1A202C),

    // Surface Containers (sections, grouped content)
    surfaceContainer = if (isDark) Color(0xFF171924).copy(alpha = 0.85f) else Color(0xFFFFFFFF),
    surfaceContainerLow = if (isDark) Color(0xFF0D0E15).copy(alpha = 0.80f) else Color(0xFFF0F3F8),
    surfaceContainerHigh = if (isDark) Color(0xFF1E2130).copy(alpha = 0.90f) else Color(0xFFFFFFFF),

    // Surface Variant (chips, tabs, secondary cards)
    surfaceVariant = if (isDark) Color(0xFF141620) else Color(0xFFEEF2F7),
    onSurfaceVariant = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),

    // Primary (keep dynamic material color)
    primary = if (isDark) primary else primary,
    onPrimary = if (isDark) onPrimary else onPrimary,
    primaryContainer = if (isDark) primaryContainer else Color(0xFFEEF2F7),
    onPrimaryContainer = if (isDark) onPrimaryContainer else Color(0xFF1A202C),

    // Secondary
    secondary = if (isDark) secondary else Color(0xFF64748B),
    onSecondary = if (isDark) onSecondary else Color(0xFFFFFFFF),
    secondaryContainer = if (isDark) secondaryContainer else Color(0xFFF0F3F8),
    onSecondaryContainer = if (isDark) onSecondaryContainer else Color(0xFF1A202C),

    // Outlines / Dividers
    outline = if (isDark) Color(0xFF282C3F) else Color(0xFFE2E8F0),
    outlineVariant = if (isDark) Color(0xFF1A1C28) else Color(0xFFF0F3F8)
)

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
