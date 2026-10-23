package com.downloader.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.model.ItemState
import com.downloader.app.core.model.ItemStatus
import com.downloader.app.core.model.JobStage
import com.downloader.app.core.service.JobController
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

@Composable
fun JobCard(
    job: JobController.UiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onCancelItem: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var confirmCancel by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(DownloaderRadius.card),
        colors = CardDefaults.cardColors(containerColor = DownloaderColors.Surface),
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JobThumbnail(job)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            label = Formatters.formatTag(job.format),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = job.qualityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(label = stageLabel(job.stage), color = stageColor(job.stage))
            }

            if (job.isActive) {
                Spacer(Modifier.height(12.dp))
                JobProgress(job)
                Spacer(Modifier.height(10.dp))
                JobActions(
                    job = job,
                    onPause = onPause,
                    onResume = onResume,
                    onCancel = { confirmCancel = true },
                )
            }

            if (job.items.size > 1) {
                JobItemList(job.items, onCancelItem)
            }

            when (job.stage) {
                JobStage.COMPLETED -> {
                    val result = job.result
                    Spacer(Modifier.height(12.dp))
                    if (result != null) {
                        Text(
                            text = result.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = DownloaderApp.str(R.string.result_saved_to, result.location),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (result.failedCount > 0) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = DownloaderApp.str(R.string.result_failed_items, result.failedCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = DownloaderColors.Danger,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { openUri(context, result.uri, result.mimeType) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(DownloaderApp.str(R.string.job_open))
                            }
                            OutlinedButton(
                                onClick = { shareUri(context, result.uri, result.mimeType) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(DownloaderApp.str(R.string.job_share))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(DownloaderApp.str(R.string.job_dismiss))
                    }
                }

                JobStage.FAILED -> {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = DownloaderColors.Danger,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = job.error?.message ?: DownloaderApp.str(R.string.err_download_generic),
                            style = MaterialTheme.typography.bodySmall,
                            color = DownloaderColors.Danger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRetry,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(DownloaderApp.str(R.string.job_retry))
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(DownloaderApp.str(R.string.job_dismiss))
                        }
                    }
                }

                JobStage.CANCELLED, JobStage.EXPIRED -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = DownloaderApp.str(R.string.stage_cancelled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(DownloaderApp.str(R.string.job_dismiss))
                    }
                }

                else -> Unit
            }
        }
    }

    MotionDialog(
        visible = confirmCancel,
        onDismiss = { confirmCancel = false },
    ) {
        Text(
            text = DownloaderApp.str(R.string.job_cancel_confirm_title),
            style = DownloaderText.dialogTitle,
            color = DownloaderColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = DownloaderApp.str(R.string.job_cancel_confirm_desc),
            style = DownloaderText.body,
            color = DownloaderColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { confirmCancel = false }) {
                Text(DownloaderApp.str(R.string.action_close))
            }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = {
                    confirmCancel = false
                    onCancel()
                },
            ) {
                Text(
                    text = DownloaderApp.str(R.string.job_cancel),
                    color = DownloaderColors.Danger,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun JobItemList(
    items: List<ItemState>,
    onCancelItem: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val doneCount = items.count { it.status == ItemStatus.COMPLETED }
    val failedCount = items.count { it.status == ItemStatus.FAILED }
    val canceledCount = items.count { it.status == ItemStatus.CANCELED }

    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DownloaderApp.str(R.string.job_items_summary, doneCount, items.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (failedCount > 0 || canceledCount > 0) {
                Spacer(Modifier.height(2.dp))
                val extras = buildList {
                    if (failedCount > 0) add(DownloaderApp.str(R.string.job_items_failed_count, failedCount))
                    if (canceledCount > 0) add(DownloaderApp.str(R.string.job_items_canceled_count, canceledCount))
                }.joinToString(" • ")
                Text(
                    text = extras,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (failedCount > 0) DownloaderColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                if (expanded) {
                    DownloaderApp.str(R.string.job_items_hide)
                } else {
                    DownloaderApp.str(R.string.job_items_show, items.size)
                },
            )
        }
    }
    if (expanded) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
        ) {
            items(items, key = { it.index }) { item ->
                ItemRow(item, onCancelItem)
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemState, onCancelItem: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${item.index}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { DownloaderApp.str(R.string.job_item_untitled, item.index) },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = itemStatusText(item),
                style = MaterialTheme.typography.labelSmall,
                color = itemStatusColor(item.status),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (item.status) {
            ItemStatus.RUNNING, ItemStatus.PROCESSING, ItemStatus.WAITING -> IconButton(
                onClick = { onCancelItem(item.index) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = DownloaderApp.str(R.string.item_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            ItemStatus.COMPLETED -> Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = DownloaderColors.Success,
                modifier = Modifier.size(16.dp),
            )

            ItemStatus.FAILED -> Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = DownloaderColors.Danger,
                modifier = Modifier.size(16.dp),
            )

            ItemStatus.CANCELED -> Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun itemStatusText(item: ItemState): String = when (item.status) {
    ItemStatus.WAITING -> DownloaderApp.str(R.string.item_waiting)
    ItemStatus.RUNNING -> if (item.percent in 0..100) {
        DownloaderApp.str(R.string.job_item_percent, item.percent)
    } else {
        DownloaderApp.str(R.string.item_running)
    }

    ItemStatus.PROCESSING -> DownloaderApp.str(R.string.item_processing)
    ItemStatus.COMPLETED -> DownloaderApp.str(R.string.item_completed)
    ItemStatus.FAILED -> item.message ?: DownloaderApp.str(R.string.item_failed)
    ItemStatus.CANCELED -> DownloaderApp.str(R.string.item_canceled)
}

@Composable
private fun itemStatusColor(status: ItemStatus) = when (status) {
    ItemStatus.RUNNING -> MaterialTheme.colorScheme.primary
    ItemStatus.PROCESSING -> DownloaderColors.Warning
    ItemStatus.COMPLETED -> DownloaderColors.Success
    ItemStatus.FAILED -> DownloaderColors.Danger
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun JobThumbnail(job: JobController.UiState) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!job.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = job.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = when (job.format) {
                    "mp3", "m4a" -> Icons.Rounded.MusicNote
                    "photo" -> Icons.Rounded.Image
                    else -> Icons.Rounded.Movie
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun JobProgress(job: JobController.UiState) {
    val percent = job.progressPercent
    val animated by animateFloatAsState(
        targetValue = ((percent ?: 0) / 100f),
        animationSpec = tween(420),
        label = "jobProgress",
    )

    Column {
        if (percent != null) {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (percent != null) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = stageLabel(job.stage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (job.speedBps > 0) {
                Text(
                    text = Formatters.speed(job.speedBps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (job.etaSeconds > 0) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = DownloaderApp.str(R.string.job_eta, Formatters.durationLong(job.etaSeconds)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (job.totalBytes > 0 && job.downloadedBytes > 0) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = DownloaderApp.str(
                    R.string.job_bytes_progress,
                    Formatters.bytes(job.downloadedBytes),
                    Formatters.bytes(job.totalBytes),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val message = job.message
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (job.totalItems > 1 && job.currentItem > 0) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = DownloaderApp.str(R.string.job_item_progress, job.currentItem, job.totalItems),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun JobActions(
    job: JobController.UiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            job.isPaused -> {
                Button(
                    onClick = onResume,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(DownloaderApp.str(R.string.job_resume))
                }
            }

            job.stage.isRunning -> {
                OutlinedButton(
                    onClick = onPause,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(DownloaderApp.str(R.string.job_pause))
                }
            }
        }
        OutlinedButton(
            onClick = onCancel,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(DownloaderApp.str(R.string.job_cancel))
        }
    }
}
