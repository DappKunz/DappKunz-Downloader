package com.downloader.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.model.JobStage
import com.downloader.app.ui.theme.DownloaderColors
import java.io.File

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmptyStateBox(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

fun stageLabel(stage: JobStage): String = when (stage) {
    JobStage.QUEUED -> DownloaderApp.str(R.string.stage_queued)
    JobStage.DOWNLOADING -> DownloaderApp.str(R.string.stage_downloading)
    JobStage.PROCESSING -> DownloaderApp.str(R.string.stage_processing)
    JobStage.PACKAGING -> DownloaderApp.str(R.string.stage_packaging)
    JobStage.PAUSED -> DownloaderApp.str(R.string.stage_paused)
    JobStage.COMPLETED -> DownloaderApp.str(R.string.stage_completed)
    JobStage.FAILED -> DownloaderApp.str(R.string.stage_failed)
    JobStage.CANCELLED, JobStage.EXPIRED -> DownloaderApp.str(R.string.stage_cancelled)
}

fun stageColor(stage: JobStage): Color = when (stage) {
    JobStage.COMPLETED -> DownloaderColors.Success
    JobStage.FAILED -> DownloaderColors.Danger
    JobStage.PAUSED -> DownloaderColors.Warning
    JobStage.CANCELLED, JobStage.EXPIRED -> DownloaderColors.TextFaint
    JobStage.QUEUED -> DownloaderColors.TextSecondary
    else -> DownloaderColors.Primary
}

fun mediaExists(context: Context, uriString: String): Boolean {
    if (uriString.isBlank()) return false
    return try {
        val uri = Uri.parse(uriString)
        when (uri.scheme?.lowercase()) {
            "file" -> File(uri.path ?: "").exists()
            "content", "android.resource" -> {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            }

            else -> true
        }
    } catch (_: Exception) {
        false
    }
}

fun openUri(context: Context, uri: String, mimeType: String) {
    if (uri.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(uri), mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, DownloaderApp.str(R.string.result_open_failed), Toast.LENGTH_LONG).show()
    }
}

fun shareUri(context: Context, uri: String, mimeType: String) {
    if (uri.isBlank()) return
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, DownloaderApp.str(R.string.result_share_chooser)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (_: Exception) {
        Toast.makeText(context, DownloaderApp.str(R.string.result_share_failed), Toast.LENGTH_SHORT).show()
    }
}

fun historyStatusLabel(status: String): String = when (status) {
    "completed" -> DownloaderApp.str(R.string.history_status_completed)
    "failed" -> DownloaderApp.str(R.string.history_status_failed)
    else -> DownloaderApp.str(R.string.history_status_cancelled)
}

fun historyStatusColor(status: String): Color = when (status) {
    "completed" -> DownloaderColors.Success
    "failed" -> DownloaderColors.Danger
    else -> DownloaderColors.TextFaint
}
