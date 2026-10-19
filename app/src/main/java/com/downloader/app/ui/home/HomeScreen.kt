package com.downloader.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.engine.EngineManager
import com.downloader.app.core.model.JobStage
import com.downloader.app.core.util.Formatters
import com.downloader.app.data.HistoryEntry
import com.downloader.app.data.HistoryStatus
import com.downloader.app.ui.components.AnimatedErrorBanner
import com.downloader.app.ui.components.ArtworkTile
import com.downloader.app.ui.components.EmptyStateBox
import com.downloader.app.ui.components.formatTagLabel
import com.downloader.app.ui.components.JobCard
import com.downloader.app.ui.components.SectionHeader
import com.downloader.app.ui.components.StatusChip
import com.downloader.app.ui.theme.ArtworkTones
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText
import com.downloader.app.ui.theme.QuickActionTones
import java.util.Calendar

/**
 * Beranda — mengikuti frame desain "Screen / 01 — Beranda" (7:2).
 *
 * Urutan dari atas: sapaan + avatar → kartu statistik → unduh berdasarkan jenis
 * → sedang berjalan → baru diunduh. Kartu "tempel tautan" pada versi lama tidak
 * ada di desain; tombol tempel tetap tersedia di layar Download.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuickType: (String?) -> Unit,
    onSeeAllHistory: () -> Unit,
    onRetry: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val engineState by EngineManager.state.collectAsStateWithLifecycle()

    val activeJobs = jobs.filter { it.isActive }
    val failedJobs = jobs.filter { it.stage == JobStage.FAILED }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = DownloaderSpacing.screen,
            end = DownloaderSpacing.screen,
            top = 18.dp,
            bottom = 118.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(DownloaderSpacing.md),
    ) {
        item { GreetingHeader() }

        val engineFailure = engineState as? EngineManager.State.Failed
        if (engineFailure != null) {
            item {
                AnimatedErrorBanner(
                    message = DownloaderApp.str(R.string.engine_failed_banner, engineFailure.message),
                    onDismiss = { },
                )
            }
        } else if (engineState is EngineManager.State.Preparing) {
            item {
                StatusChip(
                    label = DownloaderApp.str(R.string.engine_preparing),
                    color = DownloaderColors.Warning,
                )
            }
        }

        item {
            StatsRow(
                completed = stats.completed,
                active = stats.active,
                totalBytes = stats.totalBytes,
            )
        }

        item {
            SectionHeader(title = DownloaderApp.str(R.string.home_download_by_type))
        }

        item { QuickTypeRow(onQuickType = onQuickType) }

        item {
            SectionHeader(
                title = DownloaderApp.str(R.string.section_active_downloads),
                actionLabel = DownloaderApp.str(R.string.history_see_all),
                onAction = onSeeAllHistory,
            )
        }

        if (activeJobs.isEmpty() && failedJobs.isEmpty()) {
            item {
                EmptyStateBox(
                    icon = Icons.Rounded.CloudDownload,
                    title = DownloaderApp.str(R.string.home_active_empty_title),
                    description = DownloaderApp.str(R.string.home_active_empty_desc),
                )
            }
        } else {
            items(activeJobs + failedJobs, key = { it.jobId }) { job ->
                JobCard(
                    job = job,
                    onPause = { viewModel.pause(job.jobId) },
                    onResume = { viewModel.resume(job.jobId) },
                    onCancel = { viewModel.cancel(job.jobId) },
                    onRetry = { viewModel.retry(job.jobId) },
                    onCancelItem = { index -> viewModel.cancelItem(job.jobId, index) },
                    onDismiss = { viewModel.dismiss(job.jobId) },
                )
            }
        }

        item {
            SectionHeader(
                title = DownloaderApp.str(R.string.home_recent_downloads),
                actionLabel = DownloaderApp.str(R.string.history_see_all),
                onAction = onSeeAllHistory,
            )
        }

        if (history.isEmpty()) {
            item {
                EmptyStateBox(
                    icon = Icons.Rounded.History,
                    title = DownloaderApp.str(R.string.history_empty_title),
                    description = DownloaderApp.str(R.string.history_empty_desc),
                )
            }
        } else {
            items(history.take(3), key = { "h_${it.id}" }) { entry ->
                RecentDownloadRow(entry = entry, onClick = onSeeAllHistory)
            }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 4..10 -> DownloaderApp.str(R.string.greeting_morning)
        in 11..14 -> DownloaderApp.str(R.string.greeting_afternoon)
        in 15..18 -> DownloaderApp.str(R.string.greeting_evening)
        else -> DownloaderApp.str(R.string.greeting_night)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = DownloaderText.hero,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = DownloaderApp.str(R.string.greeting_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = DownloaderColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(DownloaderSpacing.md))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DownloaderColors.PrimarySoft)
                .border(1.dp, DownloaderColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun StatsRow(completed: Int, active: Int, totalBytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DownloaderSpacing.md),
    ) {
        StatCard(
            value = completed.coerceAtLeast(0).toString().padStart(2, '0'),
            label = DownloaderApp.str(R.string.stat_completed),
            valueColor = DownloaderColors.Primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = active.coerceAtLeast(0).toString().padStart(2, '0'),
            label = DownloaderApp.str(R.string.stat_active),
            valueColor = DownloaderColors.Cyan,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = Formatters.bytes(totalBytes),
            label = DownloaderApp.str(R.string.stat_total_data),
            valueColor = DownloaderColors.TextPrimary,
            compactValue = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    compactValue: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.cardLg),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier.height(DownloaderDimens.statCardHeight),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                style = if (compactValue) DownloaderText.statValueSm else DownloaderText.statValue,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = DownloaderText.rowTitleSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickTypeRow(onQuickType: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DownloaderSpacing.sm),
    ) {
        QuickTypeCard(
            icon = Icons.Rounded.Movie,
            label = DownloaderApp.str(R.string.home_type_video),
            tileColor = QuickActionTones.Video,
            iconTint = DownloaderColors.Primary,
            onClick = { onQuickType("mp4") },
            modifier = Modifier.weight(1f),
        )
        QuickTypeCard(
            icon = Icons.Rounded.MusicNote,
            label = DownloaderApp.str(R.string.home_type_audio),
            tileColor = QuickActionTones.Audio,
            iconTint = DownloaderColors.Cyan,
            onClick = { onQuickType("mp3") },
            modifier = Modifier.weight(1f),
        )
        QuickTypeCard(
            icon = Icons.Rounded.Image,
            label = DownloaderApp.str(R.string.home_type_photo),
            tileColor = QuickActionTones.Photo,
            iconTint = ArtworkTones.Violet.accent,
            onClick = { onQuickType("photo") },
            modifier = Modifier.weight(1f),
        )
        QuickTypeCard(
            icon = Icons.Rounded.Folder,
            label = DownloaderApp.str(R.string.home_type_playlist),
            tileColor = QuickActionTones.Playlist,
            iconTint = DownloaderColors.Warning,
            onClick = { onQuickType(null) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickTypeCard(
    icon: ImageVector,
    label: String,
    tileColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DownloaderRadius.md),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(DownloaderDimens.iconTile)
                    .clip(RoundedCornerShape(DownloaderRadius.iconTile))
                    .background(tileColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = DownloaderText.chip,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Baris "Baru diunduh" versi ringkas sesuai desain (353×44, radius 12):
 * artwork + judul + meta + chevron. Aksi lanjutan (buka/bagikan/hapus) ada di
 * layar Riwayat supaya baris tetap ringkas.
 */
@Composable
private fun RecentDownloadRow(entry: HistoryEntry, onClick: () -> Unit) {
    val completed = entry.status == HistoryStatus.COMPLETED
    val title = entry.fileName.ifBlank { entry.title }
    val meta = if (completed) recentMeta(entry) else historyStatusLabel(entry.status)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DownloaderRadius.artwork),
        color = DownloaderColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkTile(
                seed = title,
                modifier = Modifier.size(36.dp),
                corner = DownloaderRadius.artwork,
                label = formatTagLabel(entry.format),
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DownloaderText.chip,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    style = DownloaderText.metaSm,
                    color = if (completed) DownloaderColors.TextSecondary else DownloaderColors.Danger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = DownloaderColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun recentMeta(entry: HistoryEntry): String {
    val parts = buildList {
        add(formatTagLabel(entry.format))
        if (entry.resolution.isNotBlank() && entry.resolution != "auto") add(entry.resolution)
        if (entry.sizeBytes > 0) add(Formatters.bytes(entry.sizeBytes))
    }
    return parts.joinToString(" • ")
}

private fun historyStatusLabel(status: String): String = when (status) {
    HistoryStatus.COMPLETED -> DownloaderApp.str(R.string.history_status_completed)
    HistoryStatus.FAILED -> DownloaderApp.str(R.string.history_status_failed)
    else -> DownloaderApp.str(R.string.history_status_cancelled)
}
