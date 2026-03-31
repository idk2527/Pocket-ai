package com.pocketai.app.presentation.download

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketai.app.services.ModelDownloadManager
import kotlinx.coroutines.launch

@Composable
fun ModelDownloadScreen(
    navController: NavController,
    downloadManager: ModelDownloadManager
) {
    val progress by downloadManager.progress.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            downloadManager.downloadModels()
        }
    }

    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete) {
            navController.navigate("dashboard") {
                popUpTo("model_download") { inclusive = true }
            }
        }
    }

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val downloadedMB = progress.bytesDownloaded / (1024f * 1024f)
    val totalMB = progress.totalBytes / (1024f * 1024f)
    val fraction = if (progress.totalBytes > 0) {
        (progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing dot indicator
            val pulseAnim = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by pulseAnim.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Setting up your AI",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (progress.error != null) {
                    "Download failed. Please check your connection."
                } else if (progress.totalFiles > 0) {
                    "Downloading model ${progress.fileIndex} of ${progress.totalFiles}"
                } else {
                    "Preparing download..."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary
                                ),
                                startX = shimmerOffset * 300f,
                                endX = (shimmerOffset + 1f) * 300f
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Size counter
            Text(
                text = if (totalMB > 0) {
                    "%.0f MB / %.0f MB".format(downloadedMB, totalMB)
                } else {
                    "Connecting..."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )

            if (progress.error != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = progress.error ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
