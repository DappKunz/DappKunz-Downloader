package com.downloader.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.downloader.app.ui.theme.DownloaderColors

@Composable
fun AnimatedErrorBanner(message: String?, onDismiss: () -> Unit) {
    var lastMessage by remember { mutableStateOf("") }
    if (message != null) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(220)) + expandVertically(tween(240)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
    ) {
        ErrorBanner(message = lastMessage, onDismiss = onDismiss)
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = DownloaderColors.Danger.copy(alpha = 0.12f),
        contentColor = DownloaderColors.Danger,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AnalyzeSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    val shimmer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmer),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer),
        )
    }
}
