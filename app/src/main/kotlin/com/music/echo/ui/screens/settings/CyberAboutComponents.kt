package iad1tya.echo.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.R

// Cyber Tech Color Palette
val CyberBg = Color(0xFF080D1A)
val CyberCardBg = Color(0xFF0F1829).copy(alpha = 0.90f)
val CyberNeonCyan = Color(0xFF00F0FF)
val CyberNeonPurple = Color(0xFF9D00FF)
val CyberNeonGreen = Color(0xFF00FF9D)
val CyberNeonPink = Color(0xFFFF007F)
val CyberBorderColor = Color(0xFF1E2E4A)
val CyberTextDim = Color(0xFF8E9EB8)

@Composable
fun CyberAboutContent(
    modifier: Modifier = Modifier,
    isDialog: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "cyber_loop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isDialog) 4.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Header & Brand Banner
        CyberAppBanner(rotation = rotation, glowPulse = glowPulse)

        // 2. Creator Card ("mokka coding" with custom DP)
        CyberCreatorCard(rotation = rotation, glowPulse = glowPulse)

        // 3. Instagram Card (with Instagram DP and Link)
        CyberInstagramCard(
            onClick = {
                uriHandler.openUri("https://www.instagram.com/dark.shadow_4531?stkn=MTZua29kbXpoOGhnMA==")
            }
        )

        // 4. Developer Connect Links (GitHub & Email)
        CyberSocialGroup(
            onGithubClick = { uriHandler.openUri("https://github.com/keerthan4531-a11y") },
            onEmailClick = {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:keerthan4531@gmail.com")
                    }
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // 5. Cyber Core Architecture Specs
        CyberTechSpecsCard()

        // 6. Inixa Pixel-Art Animated Banner
        CyberInixaBannerCard()

        // If in Dialog mode, show prominent initialize action button
        if (isDialog && onDismiss != null) {
            Spacer(Modifier.height(4.dp))
            CyberButton(
                text = "INITIALIZE // ENTER BEAT BOX",
                glowPulse = glowPulse,
                onClick = onDismiss
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CyberAppBanner(
    rotation: Float,
    glowPulse: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.sweepGradient(
                listOf(
                    CyberNeonCyan.copy(alpha = 0.8f),
                    CyberNeonPurple.copy(alpha = 0.4f),
                    CyberNeonGreen.copy(alpha = 0.8f),
                    CyberNeonCyan.copy(alpha = 0.8f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // App Logo with Cyber Glow Frame
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF050811))
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(CyberNeonCyan, CyberNeonPurple, CyberNeonGreen, CyberNeonCyan)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_nobg),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = "BEAT BOX",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "// NEXT-GEN LOSSLESS AUDIO MATRIX",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = CyberNeonCyan.copy(alpha = glowPulse),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberChip(label = "VER: ${BuildConfig.VERSION_NAME}", color = CyberNeonCyan)
                CyberChip(label = BuildConfig.ARCHITECTURE.uppercase(), color = CyberNeonGreen)
                CyberChip(label = "32-BIT DSP", color = CyberNeonPurple)
            }
        }
    }
}

@Composable
fun CyberCreatorCard(
    rotation: Float,
    glowPulse: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    CyberNeonCyan.copy(alpha = 0.5f),
                    CyberBorderColor,
                    CyberNeonGreen.copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM ARCHITECT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberNeonGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberNeonGreen)
                    )
                    Text(
                        text = "ONLINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyberNeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Creator DP in Cyber Animated Frame
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        shadowElevation = 18f
                    }
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                CyberNeonCyan,
                                CyberNeonPurple,
                                CyberNeonGreen,
                                CyberNeonCyan
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF080D1A)),
                contentAlignment = Alignment.Center
            ) {
                // User face is centered in the upper third with sunglasses
                Image(
                    painter = painterResource(R.drawable.creator_dp),
                    contentDescription = "mokka coding",
                    contentScale = ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Creator Name & Bio
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "mokka coding",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberNeonCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "[ LEAD CREATOR & CORE DEVELOPER ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Creator of Beat Box • Crafting high-performance audio experiences with cutting-edge DSP & modern cyberpunk UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun CyberInstagramCard(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "insta_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = CyberNeonPink),
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    CyberNeonPink.copy(alpha = 0.8f),
                    CyberNeonPurple.copy(alpha = 0.6f),
                    CyberNeonCyan.copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMUNICATION // INSTAGRAM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberNeonPink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    painter = painterResource(R.drawable.ic_instagram_new),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = CyberNeonPink
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circular Instagram DP
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(CyberNeonPink, CyberNeonPurple, CyberNeonPink)
                            ),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF080D1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.insta_dp),
                        contentDescription = "Instagram Profile DP",
                        contentScale = ContentScale.Crop,
                        alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "@dark.shadow_4531",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Official Instagram ID • Connect & DM",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextDim
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberNeonPink.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonPink.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FOLLOW",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberNeonPink
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CyberNeonPink
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberSocialGroup(
    onGithubClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "NETWORK PROTOCOLS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = CyberNeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )

            CyberActionRow(
                icon = painterResource(R.drawable.github),
                title = "GitHub Repository",
                subtitle = "keerthan4531-a11y",
                accentColor = Color.White,
                onClick = onGithubClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = CyberBorderColor
            )

            CyberActionRow(
                icon = rememberVectorPainter(Icons.Rounded.Email),
                title = "Developer Direct Email",
                subtitle = "keerthan4531@gmail.com",
                accentColor = CyberNeonCyan,
                onClick = onEmailClick
            )
        }
    }
}

@Composable
fun CyberActionRow(
    icon: Painter,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "row_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextDim
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = CyberTextDim.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CyberTechSpecsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SYSTEM ARCHITECTURE // SPECS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = CyberNeonGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            CyberSpecLine(key = "AUDIO ENGINE", value = "32-BIT LOSSLESS FLAC / OPUS")
            CyberSpecLine(key = "DSP CORE", value = "AXION ACOUSTIC MODELLER")
            CyberSpecLine(key = "PLAYBACK ENGINE", value = "MEDIA3 EXOPLAYER 2026")
            CyberSpecLine(key = "PRIVACY STATUS", value = "100% LOCAL & TELEMETRY-FREE")
        }
    }
}

@Composable
fun CyberSpecLine(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberTextDim,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CyberChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CyberButton(
    text: String,
    glowPulse: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(CyberNeonCyan, CyberNeonGreen, CyberNeonCyan)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF003B46),
            contentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CyberNeonGreen)
            )
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun CyberInixaBannerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    CyberNeonPurple.copy(alpha = 0.6f),
                    CyberNeonCyan.copy(alpha = 0.6f)
                )
            )
        )
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(ctx.assets, "lmeb_inixa.gif")
                            val drawable = ImageDecoder.decodeDrawable(source)
                            setImageDrawable(drawable)
                            if (drawable is AnimatedImageDrawable) {
                                drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                drawable.start()
                            }
                        } else {
                            val stream = ctx.assets.open("lmeb_inixa.gif")
                            val bmp = BitmapFactory.decodeStream(stream)
                            setImageBitmap(bmp)
                        }
                    } catch (e: Exception) {
                        Timber.tag("CyberInixaBanner").e(e, "Failed to load inixa gif")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        )
    }
}
