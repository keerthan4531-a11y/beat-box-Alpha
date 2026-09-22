package iad1tya.echo.music.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import android.widget.Toast
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.ui.screens.settings.CyberAboutContent
import iad1tya.echo.music.ui.screens.settings.CyberBg
import iad1tya.echo.music.ui.screens.settings.CyberNeonCyan

@Composable
fun WelcomeDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = CyberBg
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        CyberNeonCyan.copy(alpha = 0.8f),
                        androidx.compose.ui.graphics.Color(0xFF9D00FF).copy(alpha = 0.6f),
                        CyberNeonCyan.copy(alpha = 0.8f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 12.dp)
            ) {
                CyberAboutContent(
                    isDialog = true,
                    onDismiss = onDismissRequest
                )
            }
        }
    }
}
