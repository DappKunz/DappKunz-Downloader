package com.downloader.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import com.downloader.app.data.HistoryEntry
import com.downloader.app.data.HistoryStatus
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Kartu riwayat sesuai desain (353×68, radius 14, fill Surface + border):
 * artwork 48 (radius 10), nama berkas 11·500, metadata 9·400, status 9·500
 * berwarna, waktu di kanan (9·400), dan menu aksi.
 *
 * Aksi lama dipertahankan (buka, bagikan, coba lagi, hapus) — desain hanya
 * memuat Buka/Bagikan/Hapus, sedangkan "Coba lagi" dibutuhkan untuk unduhan
 * yang gagal.
 */
@Composable
fun HistoryItem(
    entry: HistoryEntry,
    onRetry: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val isCompleted = entry.status == HistoryStatus.COMPLETED && entry.uri.isNotBlank()
    val name = entry.fileName.ifBlank { entry.title }

    Surface(
        shape = RoundedCornerShape(DownloaderRadius.md),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(start = 7.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(DownloaderDimens.artworkLg)) {
                ArtworkTile(
                    seed = name,
                    modifier = Modifier.matchParentSize(),
                    corner = DownloaderRadius.tileSmall,
                    label = formatTagLabel(entry.format),
                )
                if (!entry.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(DownloaderRadius.tileSmall)),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 7.dp),
            ) {
                Text(
                    text = name,
                    style = DownloaderText.rowTitleSm,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = metadataLine(entry),
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = historyStatusLabel(entry.status),
                        style = DownloaderText.status,
                        color = historyStatusColor(entry.status),
                        maxLines = 1,
                    )
                    if (entry.status == HistoryStatus.FAILED && !entry.errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = entry.errorMessage,
                            style = DownloaderText.metaSm,
                            color = DownloaderColors.Danger,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .height(54.dp)
                    .padding(start = 6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = historyTimeLabel(entry.createdAt),
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Box {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(DownloaderRadius.pill))
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = DownloaderApp.str(R.string.player_more_actions),
                            tint = DownloaderColors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(DownloaderRadius.menu),
                        containerColor = DownloaderColors.SurfaceElevated,
                        border = BorderStroke(1.dp, DownloaderColors.Border),
                    ) {
                        if (isCompleted) {
                            DropdownMenuItem(
                                text = { Text(DownloaderApp.str(R.string.job_open)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    if (mediaExists(context, entry.uri)) {
                                        openUri(context, entry.uri, entry.mimeType)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            DownloaderApp.str(R.string.history_file_missing),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(DownloaderApp.str(R.string.job_share)) },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    if (mediaExists(context, entry.uri)) {
                                        shareUri(context, entry.uri, entry.mimeType)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            DownloaderApp.str(R.string.history_file_missing),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                            )
                        }
                        if (entry.url.isNotBlank() && entry.status != HistoryStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text(DownloaderApp.str(R.string.history_retry)) },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onRetry(entry)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    DownloaderApp.str(R.string.action_delete),
                                    color = DownloaderColors.Danger,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = DownloaderColors.Danger,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete(entry)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Metadata baris: "MP4 • 1080p • 27 MB" atau "MP3 • 320 kbps • 15,1 MB". */
private fun metadataLine(entry: HistoryEntry): String = buildString {
    append(Formatters.formatTag(entry.format))
    val quality = entry.qualityLabel.ifBlank {
        when {
            entry.resolution.isNotBlank() && entry.resolution != "auto" -> entry.resolution
            entry.bitrate > 0 && (entry.format == "mp3" || entry.format == "m4a") ->
                "${entry.bitrate} kbps"

            else -> ""
        }
    }
    if (quality.isNotBlank()) append(" • ").append(quality)
    if (entry.sizeBytes > 0) append(" • ").append(Formatters.bytes(entry.sizeBytes))
}

/** Waktu ringkas: "10:46" hari ini, "Kemarin", atau "3 Mei" untuk hari lain. */
private fun historyTimeLabel(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    if (sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameYear && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return DownloaderApp.str(R.string.history_group_yesterday)
    }
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
}
