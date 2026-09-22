package iad1tya.echo.music.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ultra-Luxury Neomorphism 2.0 Design System Engine
 *
 * LIGHT MODE — "Ivory Porcelain" Philosophy:
 * - Canvas: Warm luminous off-white (#F5F7FA)
 * - Cards: Pure white (#FFFFFF) with subtle blue-grey soft shadows
 * - Dual-shadow technique: White highlight top-left, soft grey shadow bottom-right
 * - Shadows are DIFFUSED (high blur, low opacity) for premium floating feel
 * - Rim borders: Near-invisible white-to-transparent gradient
 *
 * DARK MODE — "Obsidian Titanium" (unchanged from original)
 */
object NeomorphDefaults {
    // ==========================================
    // Light Mode Palette — "Ivory Porcelain"
    // ==========================================
    val LightCanvas = Color(0xFFF5F7FA)         // Warm ivory background
    val LightSurface = Color(0xFFFFFFFF)         // Pure white cards
    val LightSurfaceElevated = Color(0xFFFFFFFF) // Pure white elevated
    
    // Shadows: Ultra-soft, diffused blue-grey (NEVER dark/muddy)
    val LightShadowDark = Color(0xFF94A3B8).copy(alpha = 0.20f)   // Soft bottom-right
    val LightShadowWhite = Color(0xFFFFFFFF).copy(alpha = 1.0f)   // Crisp top-left highlight

    // ==========================================
    // Dark Mode Palette — "Obsidian Titanium"
    // ==========================================
    val DarkCanvas = Color(0xFF090A0F)
    val DarkSurface = Color(0xFF10121A)
    val DarkSurfaceElevated = Color(0xFF171924)
    val DarkShadowDark = Color(0xFF040508).copy(alpha = 0.85f)
    val DarkShadowLight = Color(0xFF202436).copy(alpha = 0.45f)

    // Shared Radii
    val CardRadius = 24.dp
    val ButtonRadius = 18.dp
    val PillRadius = 32.dp
    val SmallRadius = 12.dp
}

/**
 * Convex (Extruded / Raised) 3D Surface
 *
 * Light mode: Pure white card that appears to float above the ivory canvas
 * with a soft bottom-right shadow and crisp top-left highlight edge.
 * The card feels PHYSICALLY LIFTED like a premium ceramic tile.
 */
@Composable
fun Modifier.neomorphicConvex(
    cornerRadius: Dp = NeomorphDefaults.CardRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    elevation: Dp = 6.dp,
    accentTint: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = isAppInDarkTheme()

    // Surface gradient: subtle directional light
    val surfaceBrush = remember(isDark, accentTint) {
        if (accentTint != Color.Unspecified) {
            Brush.linearGradient(
                colors = listOf(
                    accentTint.copy(alpha = if (isDark) 0.35f else 0.12f),
                    if (isDark) NeomorphDefaults.DarkSurfaceElevated else Color(0xFFFFFFFF)
                )
            )
        } else {
            Brush.linearGradient(
                colors = if (isDark) listOf(
                    Color(0xFF181A26),
                    Color(0xFF0D0E15)
                ) else listOf(
                    Color(0xFFFFFFFF),   // Pure white top-left
                    Color(0xFFFCFDFE)    // Barely-off-white bottom-right
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    // Rim border: subtle edge definition
    val rimBrush = remember(isDark) {
        Brush.linearGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent,
                Color.White.copy(alpha = 0.10f)
            ) else listOf(
                Color.White.copy(alpha = 1.0f),     // Bright highlight edge
                Color.White.copy(alpha = 0.80f),
                Color(0xFFE2E8F0).copy(alpha = 0.15f),  // Very subtle grey at bottom
                Color(0xFFE2E8F0).copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    return this
        .shadow(
            elevation = if (isDark) elevation else elevation * 1.1f,
            shape = shape,
            clip = false,
            spotColor = if (isDark) NeomorphDefaults.DarkShadowDark else NeomorphDefaults.LightShadowDark,
            ambientColor = if (isDark) NeomorphDefaults.DarkShadowDark else NeomorphDefaults.LightShadowDark
        )
        .background(brush = surfaceBrush, shape = shape)
        .border(width = borderWidth, brush = rimBrush, shape = shape)
        .clip(shape)
}

/**
 * Concave / Inset (Sunken) 3D Surface
 *
 * Light mode: A gentle depression carved into the ivory porcelain,
 * with inverted shadows — dark at top-left, bright at bottom-right.
 * Used for search inputs, active tabs, sliders, and selected chips.
 */
@Composable
fun Modifier.neomorphicInset(
    cornerRadius: Dp = NeomorphDefaults.ButtonRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    accentTint: Color = Color.Unspecified
): Modifier {
    val isDark = isAppInDarkTheme()

    val insetGradient = remember(isDark, accentTint) {
        if (accentTint != Color.Unspecified) {
            Brush.linearGradient(
                colors = listOf(
                    accentTint.copy(alpha = if (isDark) 0.25f else 0.10f),
                    if (isDark) Color(0xFF090A10) else Color(0xFFEDF1F7)
                )
            )
        } else {
            Brush.linearGradient(
                colors = if (isDark) listOf(
                    Color(0xFF090A10),
                    Color(0xFF141622)
                ) else listOf(
                    Color(0xFFEDF1F7),   // Slightly darker than canvas (sunken)
                    Color(0xFFF5F7FA)    // Fades back to canvas color
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    val innerBorder = remember(isDark) {
        Brush.linearGradient(
            colors = if (isDark) listOf(
                Color.Black.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.12f)
            ) else listOf(
                Color(0xFFD1D9E6).copy(alpha = 0.30f),  // Soft shadow edge (top-left)
                Color.White.copy(alpha = 0.95f)          // Bright highlight (bottom-right)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    return this
        .background(brush = insetGradient, shape = shape)
        .border(width = 1.dp, brush = innerBorder, shape = shape)
        .clip(shape)
}

/**
 * Tactile Neomorphic Button
 * Seamlessly transitions from 3D Convex to 3D Inset when pressed with bouncy spring physics.
 */
@Composable
fun Modifier.neomorphicButton(
    cornerRadius: Dp = NeomorphDefaults.ButtonRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    accentTint: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "neoButtonScale"
    )

    val baseModifier = if (isPressed) {
        this.neomorphicInset(cornerRadius = cornerRadius, shape = shape, accentTint = accentTint)
    } else {
        this.neomorphicConvex(cornerRadius = cornerRadius, shape = shape, elevation = 5.dp, accentTint = accentTint)
    }

    return baseModifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Circular Neomorphic Tactile Button
 */
@Composable
fun Modifier.neomorphicCircleButton(
    accentTint: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    return this.neomorphicButton(
        cornerRadius = 100.dp,
        shape = CircleShape,
        accentTint = accentTint,
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Neomorphic Container Card
 */
@Composable
fun Modifier.neomorphicCard(
    cornerRadius: Dp = NeomorphDefaults.CardRadius,
    elevation: Dp = 6.dp,
    accentTint: Color = Color.Unspecified
): Modifier {
    return this.neomorphicConvex(
        cornerRadius = cornerRadius,
        shape = RoundedCornerShape(cornerRadius),
        elevation = elevation,
        accentTint = accentTint
    )
}

