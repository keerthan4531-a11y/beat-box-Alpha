package iad1tya.echo.music.ui.screens.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.theme.neomorphicCard
import iad1tya.echo.music.ui.theme.neomorphicConvex
import iad1tya.echo.music.ui.theme.neomorphicInset
import iad1tya.echo.music.ui.theme.liquidGlass
import iad1tya.echo.music.ui.theme.isAppInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController? = null) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    // ── Collect real data from DB ──────────────────────────────────────────────
    val likedSongCount by database.likedSongsCount().collectAsState(initial = 0)
    val eventCount by database.eventCount().collectAsState(initial = 0)
    val allTimePlayTimeMs by database.getTotalPlayTimeInRange(0, Long.MAX_VALUE)
        .collectAsState(initial = 0L)
    val uniqueSongs by database.getUniqueSongCountInRange(0, Long.MAX_VALUE)
        .collectAsState(initial = 0)
    val uniqueArtists by database.getUniqueArtistCountInRange(0, Long.MAX_VALUE)
        .collectAsState(initial = 0)

    // Current player state
    val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    // ── Animated background gradient ───────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "profileBg")
    val gradientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientPhase"
    )

    // Tab state
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Stats", "Settings")

    val totalPlayTime = allTimePlayTimeMs ?: 0L
    val totalHours = (totalPlayTime / 3600000).toInt()
    val totalMinutes = ((totalPlayTime % 3600000) / 60000).toInt()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Animated Aurora Background ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = size.width / 2
                    val cy = size.height / 3
                    val radius = size.width * 0.8f
                    val offset = gradientPhase * (Math.PI / 180f).toFloat()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(
                                cx + radius * 0.3f * kotlin.math.cos(offset),
                                cy + radius * 0.2f * kotlin.math.sin(offset)
                            ),
                            radius = radius * 0.6f
                        ),
                        radius = radius
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(
                                cx - radius * 0.4f * kotlin.math.cos(offset * 0.7f),
                                cy + radius * 0.5f * kotlin.math.sin(offset * 1.3f)
                            ),
                            radius = radius * 0.5f
                        ),
                        radius = radius
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(
                                cx + radius * 0.2f * kotlin.math.sin(offset * 1.5f),
                                cy + radius * 0.6f
                            ),
                            radius = radius * 0.4f
                        ),
                        radius = radius
                    )
                }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile Header ─────────────────────────────────────────────────
            item {
                ProfileHeaderGlass(
                    likedCount = likedSongCount,
                    totalSongs = uniqueSongs,
                    totalArtists = uniqueArtists,
                    hoursListened = totalHours,
                    minutesListened = totalMinutes,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    tertiaryColor = tertiaryColor,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    surfaceVariant = surfaceVariant
                )
            }

            // ── Now Playing Glass Card ─────────────────────────────────────────
            item {
                val meta = mediaMetadata
                if (meta != null) {
                    NowPlayingGlassCard(
                        title = meta.title?.toString() ?: "Unknown",
                        artist = meta.artists?.joinToString { it.name } ?: "Unknown",
                        thumbnailUrl = meta.thumbnailUrl,
                        isPlaying = isPlaying,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        tertiaryColor = tertiaryColor,
                        onSurface = onSurface,
                        onSurfaceVariant = onSurfaceVariant
                    )
                }
            }

            // ── Tab Row ────────────────────────────────────────────────────────
            item {
                GlassTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    primaryColor = primaryColor,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant
                )
            }

            // ── Tab Content ────────────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    // Overview Tab
                    item {
                        OverviewContent(
                            likedSongs = likedSongCount,
                            eventCount = eventCount,
                            uniqueSongs = uniqueSongs,
                            uniqueArtists = uniqueArtists,
                            totalHours = totalHours,
                            totalMinutes = totalMinutes,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            onSurface = onSurface,
                            onSurfaceVariant = onSurfaceVariant
                        )
                    }
                }
                1 -> {
                    // Stats Tab
                    item {
                        StatsOverviewGrid(
                            totalSongs = uniqueSongs,
                            totalArtists = uniqueArtists,
                            likedSongs = likedSongCount,
                            totalHours = totalHours,
                            totalMinutes = totalMinutes,
                            eventCount = eventCount,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            onSurface = onSurface,
                            onSurfaceVariant = onSurfaceVariant
                        )
                    }
                }
                2 -> {
                    // Settings shortcuts
                    item {
                        SettingsShortcutsCard(
                            navController = navController,
                            onSurface = onSurface,
                            onSurfaceVariant = onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// ── Premium Glass Components ────────────────────────────────────────────────────
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderGlass(
    likedCount: Int,
    totalSongs: Int,
    totalArtists: Int,
    hoursListened: Int,
    minutesListened: Int,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    surfaceVariant: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neomorphicCard(cornerRadius = 28.dp, elevation = 6.dp)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with animated glow ring
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
                    val ringRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(6000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ringRotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer { rotationZ = ringRotation }
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        primaryColor,
                                        secondaryColor,
                                        tertiaryColor,
                                        primaryColor
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = "Avatar",
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Beat Box Listener",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                Text(
                    text = "Music Enthusiast • Premium Vibes",
                    fontSize = 13.sp,
                    color = onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatItem(Icons.Rounded.Favorite, "$likedCount", "Liked", onSurface, onSurfaceVariant)
                    MiniStatItem(Icons.Rounded.MusicNote, "$totalSongs", "Songs", onSurface, onSurfaceVariant)
                    MiniStatItem(Icons.Rounded.Mic, "$totalArtists", "Artists", onSurface, onSurfaceVariant)
                    MiniStatItem(Icons.Rounded.Timer, "${hoursListened}h ${minutesListened}m", "Listened", onSurface, onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MiniStatItem(icon: ImageVector, value: String, label: String, onSurface: Color, onSurfaceVariant: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = onSurface, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = onSurfaceVariant
        )
    }
}

@Composable
private fun NowPlayingGlassCard(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .liquidGlass(cornerRadius = 24.dp, glassAlpha = 0.85f, elevation = 6.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(onSurface.copy(alpha = 0.08f))
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Headphones else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = if (isPlaying) tertiaryColor else onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Now Playing" else "Paused",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPlaying) tertiaryColor else onSurfaceVariant
                    )
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    fontSize = 13.sp,
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isPlaying) {
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(pulse)
                        .clip(CircleShape)
                        .background(tertiaryColor)
                )
            }
        }
    }
}

@Composable
private fun GlassTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    primaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .neomorphicInset(cornerRadius = 20.dp)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val tabModifier = if (isSelected) {
                    Modifier
                        .weight(1f)
                        .neomorphicConvex(cornerRadius = 16.dp, elevation = 3.dp, accentTint = primaryColor)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp)
                }

                Box(
                    modifier = tabModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) onSurface else onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(
    likedSongs: Int,
    eventCount: Int,
    uniqueSongs: Int,
    uniqueArtists: Int,
    totalHours: Int,
    totalMinutes: Int,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Insight Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neomorphicCard(cornerRadius = 24.dp, elevation = 5.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, tint = onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listening Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've played $eventCount tracks so far, " +
                            "exploring $uniqueSongs unique songs across $uniqueArtists artists. " +
                            if (totalHours > 10) "You're a true music aficionado!"
                            else if (totalHours > 1) "Great taste in music! Keep it going."
                            else "Start your musical journey!",
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        // Activity Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsGlassCard(
                icon = Icons.Rounded.Headphones,
                title = "Total Listening",
                value = "${totalHours}h ${totalMinutes}m",
                gradient = listOf(primaryColor, secondaryColor),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            StatsGlassCard(
                icon = Icons.Rounded.Favorite,
                title = "Liked Songs",
                value = "$likedSongs",
                gradient = listOf(tertiaryColor, Color(0xFFEF4444)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Mood chips
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neomorphicCard(cornerRadius = 22.dp, elevation = 4.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Current Vibe", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair(Icons.Rounded.MusicNote, "Eclectic"),
                        Pair(Icons.Rounded.Spa, "Chill"),
                        Pair(Icons.Rounded.LocalFireDepartment, "Energetic")
                    ).forEach { (icon, mood) ->
                        Box(
                            modifier = Modifier
                                .neomorphicConvex(cornerRadius = 20.dp, elevation = 2.dp, accentTint = primaryColor)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = onSurface, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mood, fontSize = 12.sp, color = onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewGrid(
    totalSongs: Int,
    totalArtists: Int,
    likedSongs: Int,
    totalHours: Int,
    totalMinutes: Int,
    eventCount: Int,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsGlassCard(
                icon = Icons.Rounded.Headphones,
                title = "Total Listening",
                value = "${totalHours}h ${totalMinutes}m",
                gradient = listOf(primaryColor, secondaryColor),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            StatsGlassCard(
                icon = Icons.Rounded.Favorite,
                title = "Liked Songs",
                value = "$likedSongs",
                gradient = listOf(tertiaryColor, Color(0xFFEF4444)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsGlassCard(
                icon = Icons.Rounded.MusicNote,
                title = "Unique Songs",
                value = "$totalSongs",
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            StatsGlassCard(
                icon = Icons.Rounded.Mic,
                title = "Artists Explored",
                value = "$totalArtists",
                gradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsGlassCard(
                icon = Icons.Rounded.BarChart,
                title = "Total Plays",
                value = "$eventCount",
                gradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            StatsGlassCard(
                icon = Icons.Rounded.Bolt,
                title = "Avg/Day",
                value = if (eventCount > 0) "${eventCount / maxOf(1, totalHours / 24)}" else "0",
                gradient = listOf(Color(0xFFF97316), Color(0xFFEA580C)),
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Taste Analyzer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neomorphicCard(cornerRadius = 24.dp, elevation = 5.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Taste Analyzer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've explored $totalSongs unique tracks across $totalArtists artists. " +
                            if (totalHours > 10) "You're a dedicated listener!"
                            else if (totalHours > 1) "Great taste in music! Keep it going."
                            else "Start your musical journey!",
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StatsGlassCard(
    icon: ImageVector,
    title: String,
    value: String,
    gradient: List<Color>,
    onSurface: Color,
    onSurfaceVariant: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neomorphicCard(cornerRadius = 22.dp, elevation = 4.dp)
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = onSurface, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsShortcutsCard(navController: NavController?, onSurface: Color, onSurfaceVariant: Color) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Icon(Icons.Rounded.Settings, contentDescription = null, tint = onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Quick Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
        }

        val shortcuts = listOf(
            Triple(Icons.Rounded.Palette, "Appearance", "settings/appearance"),
            Triple(Icons.Rounded.MusicNote, "Player Settings", "settings/player"),
            Triple(Icons.Rounded.Lock, "Privacy", "settings/privacy"),
            Triple(Icons.Rounded.Storage, "Storage", "settings/storage"),
            Triple(Icons.Rounded.Info, "About Beat Box", "settings/about")
        )

        shortcuts.forEach { (icon, title, route) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(onSurface.copy(alpha = 0.04f))
                    .border(1.dp, onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { navController?.navigate(route) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = onSurface, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
