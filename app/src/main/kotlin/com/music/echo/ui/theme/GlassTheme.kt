package iad1tya.echo.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = staticCompositionLocalOf<HazeState> {
    error("No HazeState provided")
}

@Composable
fun ProvideHazeState(
    hazeState: HazeState = remember { HazeState() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        content()
    }
}

/**
 * Adaptive Glassmorphism / Neomorphism Modifier.
 * In Dark Mode: Sleek Apple Obsidian Glass.
 * In Light Mode: Ultra-Luxury Tactile Porcelain Neomorphism (Soft 3D).
 */
@Composable
fun Modifier.glassmorphismEffect(
    cornerRadius: Dp = 0.dp,
    alpha: Float = 0.15f
): Modifier {
    val isDark = isAppInDarkTheme()
    
    if (!isDark) {
        if (cornerRadius == 0.dp) {
            // Flat translucent blur for TopBar in light mode without heavy 3D edges
            return this
        }
        // True 3D Porcelain Neomorphism for floating cards & navigation pills in Light Mode
        return this.neomorphicConvex(
            cornerRadius = cornerRadius,
            shape = RoundedCornerShape(cornerRadius),
            elevation = 5.dp
        )
    }

    val hazeState = LocalHazeState.current
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = alpha * 1.5f),
            Color.White.copy(alpha = alpha * 0.1f)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.05f)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    return this
        .hazeChild(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color(0xFF090A0F),
                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.45f))),
                blurRadius = 40.dp
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .background(brush, shape = RoundedCornerShape(cornerRadius))
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = RoundedCornerShape(cornerRadius)
        )
        .clip(RoundedCornerShape(cornerRadius))
}

/**
 * Modifier to mark the background that should be blurred.
 */
@Composable
fun Modifier.glassmorphismSource(): Modifier {
    val isDark = isAppInDarkTheme()
    if (!isDark) return this
    val hazeState = LocalHazeState.current
    return this.haze(hazeState)
}

/**
 * Bouncing Touch modifier for high-end micro-interactions.
 */
@Composable
fun Modifier.bouncingClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * A combined modifier that gives a composable the complete Card look:
 * In Dark: Obsidian Glass with soft shadow.
 * In Light: Physical 3D extruded porcelain card with dual-light physics.
 */
@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.10f,
    onClick: (() -> Unit)? = null
): Modifier {
    val isDark = isAppInDarkTheme()
    
    var modifier = if (isDark) {
        this
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .glassmorphismEffect(cornerRadius, alpha)
    } else {
        this.neomorphicConvex(
            cornerRadius = cornerRadius,
            shape = RoundedCornerShape(cornerRadius),
            elevation = 5.dp
        )
    }
        
    if (onClick != null) {
        modifier = modifier.bouncingClickable(onClick = onClick)
    }
    return modifier
}
