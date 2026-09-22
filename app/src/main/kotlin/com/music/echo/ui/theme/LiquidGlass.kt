package iad1tya.echo.music.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-inspired Liquid Glass & Neomorphism Design Tokens
 * Engineered for $100-tier ultra-luxury aesthetic + 60/120 FPS performance across all devices.
 */
object LiquidGlassDefaults {
    val CardCornerRadius = 24.dp
    val PillCornerRadius = 32.dp
    val DialogCornerRadius = 28.dp
    val ButtonCornerRadius = 20.dp

    // Deep luxury obsidian glass colors
    val DarkGlassBackground = Color(0xFF0F1017).copy(alpha = 0.72f)
    val LightGlassBackground = Color(0xFFF2F4F8).copy(alpha = 0.65f)
    val CrystalGlassBackground = Color(0xFF141522).copy(alpha = 0.60f)

    // Specular refraction highlight borders
    val SpecularTopHighlight = Color.White.copy(alpha = 0.38f)
    val SpecularMidHighlight = Color.White.copy(alpha = 0.12f)
    val SpecularBottomHighlight = Color.White.copy(alpha = 0.03f)

    // Neomorphic dual-light shadows
    val AmbientShadowDark = Color(0x66000000)
    val AmbientShadowLight = Color(0x22000000)
    val SoftGlowColor = Color(0xFF7C4DFF).copy(alpha = 0.25f)
}

/**
 * Core Liquid Glass Modifier:
 * Combines frosted semi-translucent glass, refraction edge borders, and ambient drop shadows.
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = LiquidGlassDefaults.CardCornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    glassAlpha: Float = 0.75f,
    tintColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
): Modifier {
    val isDark = isAppInDarkTheme()

    val surfaceColor = remember(isDark, glassAlpha, tintColor) {
        val base = if (isDark) Color(0xFF10121A) else Color(0xFFFFFFFF)
        if (tintColor != Color.Unspecified) {
            tintColor.copy(alpha = (glassAlpha * 0.4f).coerceIn(0.1f, 0.9f))
        } else {
            base.copy(alpha = if (isDark) glassAlpha else 0.82f)
        }
    }

    // Specular edge border gradient simulating physical light refraction on glass edges
    val refractionBorder = remember(isDark) {
        Brush.linearGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.02f),
                Color.White.copy(alpha = 0.15f)
            ) else listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.45f),
                Color(0xFFE2E8F0).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.60f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            spotColor = if (isDark) LiquidGlassDefaults.AmbientShadowDark else Color(0x1F94A3B8),
            ambientColor = if (isDark) LiquidGlassDefaults.AmbientShadowDark else Color(0x1594A3B8)
        )
        .background(surfaceColor, shape = shape)
        .border(width = borderWidth, brush = refractionBorder, shape = shape)
        .clip(shape)
}

/**
 * Neomorphic Liquid Glass Modifier:
 * Emulates physical soft convex/embossed glass surfaces with dual-point lighting.
 */
@Composable
fun Modifier.neomorphicGlass(
    cornerRadius: Dp = LiquidGlassDefaults.ButtonCornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    accentColor: Color = Color.Unspecified,
    isPressed: Boolean = false,
): Modifier {
    val isDark = isAppInDarkTheme()

    val surfaceBrush = remember(isDark, accentColor, isPressed) {
        if (isPressed) {
            Brush.linearGradient(
                colors = if (isDark) listOf(
                    Color(0xFF0A0B10),
                    Color(0xFF141520)
                ) else listOf(
                    Color(0xFFE2E6EE),
                    Color(0xFFF8FAFD)
                )
            )
        } else {
            val topColor = if (accentColor != Color.Unspecified) {
                accentColor.copy(alpha = if (isDark) 0.35f else 0.25f)
            } else if (isDark) Color(0xFF1E2030).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f)

            val bottomColor = if (isDark) Color(0xFF10111A).copy(alpha = 0.90f) else Color(0xFFF0F3F8).copy(alpha = 0.85f)

            Brush.verticalGradient(
                colors = listOf(topColor, bottomColor)
            )
        }
    }

    val specularBorder = remember(isDark, isPressed) {
        Brush.linearGradient(
            colors = if (isPressed) listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.20f)
            ) else listOf(
                Color.White.copy(alpha = if (isDark) 0.50f else 0.95f),
                Color.White.copy(alpha = if (isDark) 0.05f else 0.20f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    return this
        .shadow(
            elevation = if (isPressed) 2.dp else 6.dp,
            shape = shape,
            clip = false,
            spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x1F94A3B8)
        )
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 1.dp, brush = specularBorder, shape = shape)
        .clip(shape)
}

/**
 * Tactile Spring Press Physics:
 * Provides realistic physical glass compression bounce on touch.
 */
@Composable
fun Modifier.liquidGlassPressable(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glassSpringScale"
    )

    return this
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
 * Dynamic Ambient Glow:
 * Subtle chromatic halo surrounding the element that matches song/artist artwork palette.
 */
@Composable
fun Modifier.liquidGlassGlow(
    glowColor: Color,
    cornerRadius: Dp = LiquidGlassDefaults.CardCornerRadius,
    glowRadius: Dp = 16.dp,
): Modifier {
    val glowBrush = remember(glowColor) {
        Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.35f),
                glowColor.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }

    return this
        .shadow(
            elevation = glowRadius,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            spotColor = glowColor.copy(alpha = 0.4f),
            ambientColor = glowColor.copy(alpha = 0.2f)
        )
}
