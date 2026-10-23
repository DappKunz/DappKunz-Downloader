package com.downloader.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.model.NormalizedMetadata
import com.downloader.app.core.model.PlaylistInfo
import com.downloader.app.core.model.PlaylistItem
import com.downloader.app.core.util.Formatters
import com.downloader.app.core.util.SizeEstimator
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

val BitrateOptions = listOf(320, 256, 192, 160, 128, 96, 64)

private fun mediaTypeLabel(type: String): String = when (type) {
    "audio" -> DownloaderApp.str(R.string.media_type_audio)
    "photo", "image" -> DownloaderApp.str(R.string.media_type_photo)
    "playlist" -> DownloaderApp.str(R.string.media_type_playlist)
    else -> DownloaderApp.str(R.string.media_type_video)
}

/**
 * Kartu pratinjau hasil analisis (desain: 353×117, radius 16, stroke `#2B3242`).
 *
 * Artwork tampil sebagai **kotak 88×88** sesuai desain — bukan banner 16:9 —
 * sehingga cover album/thumbnail lagu tidak terlihat melebar (perbaikan bug).
 */
@Composable
fun MediaPreview(meta: NormalizedMetadata) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.cardLg),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumb = meta.media.thumbnail
            Box(modifier = Modifier.size(DownloaderDimens.artworkXl)) {
                ArtworkTile(
                    seed = meta.media.title,
                    modifier = Modifier.matchParentSize(),
                    corner = DownloaderRadius.artwork,
                    label = mediaTypeLabel(meta.media.type).uppercase(),
                    labelStyle = DownloaderText.chip,
                )
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(DownloaderRadius.artwork)),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.media.title,
                    style = DownloaderText.cardTitle,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val source = mediaSourceLine(meta)
                if (source != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = source,
                        style = DownloaderText.meta,
                        color = DownloaderColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewBadge(
                        icon = Icons.Rounded.Check,
                        label = DownloaderApp.str(R.string.media_preview_ready),
                        containerColor = DownloaderColors.SuccessSoft,
                        contentColor = DownloaderColors.Success,
                    )
                    meta.playlist?.let { playlist ->
                        PreviewBadge(
                            icon = null,
                            label = DownloaderApp.str(R.string.badge_item_count, playlist.itemCount),
                            containerColor = DownloaderColors.WarningSoft,
                            contentColor = DownloaderColors.Warning,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Baris sumber desain ("Creator Channel • 03:47").
 * Platform dipakai sebagai ganti bila nama kreator tidak tersedia; durasi
 * tidak diulang untuk playlist karena sudah ada di ringkasan playlist.
 */
private fun mediaSourceLine(meta: NormalizedMetadata): String? {
    val parts = buildList {
        val creator = meta.media.creator?.takeIf { it.isNotBlank() }
            ?: meta.media.artist?.takeIf { it.isNotBlank() }
        add(creator ?: Formatters.platformLabel(meta.source.platform))
        val duration = meta.media.duration
        if (duration != null && duration > 0 && meta.playlist == null) {
            add(Formatters.duration(duration))
        }
    }
    return parts.joinToString(" • ").takeIf { it.isNotBlank() }
}

/** Badge kecil gaya desain (tinggi 24, radius pill, isian solid + teks aksen). */
@Composable
private fun PreviewBadge(
    icon: ImageVector?,
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.pill),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(text = label, style = DownloaderText.chip)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatPills(meta: NormalizedMetadata, selected: String, onSelect: (String) -> Unit) {
    val caps = meta.capabilities
    val options = listOf(
        FormatOption("mp4", "MP4", Icons.Rounded.Movie, caps.mp4 || caps.video),
        FormatOption("mp3", "MP3", Icons.Rounded.MusicNote, caps.mp3 || caps.audio),
        FormatOption("m4a", "M4A", Icons.Rounded.MusicNote, caps.m4a || caps.audio),
        FormatOption(
            "photo",
            DownloaderApp.str(R.string.media_type_photo),
            Icons.Rounded.Image,
            caps.photo || caps.image,
        ),
    )

    Column {
        Text(
            text = DownloaderApp.str(R.string.format_title),
            style = DownloaderText.sectionTitleSm,
            color = DownloaderColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                DownloaderChip(
                    label = option.label,
                    selected = selected == option.id,
                    enabled = option.enabled,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }
}

private data class FormatOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
)

@Composable
fun QualitySection(
    meta: NormalizedMetadata,
    format: String,
    bitrate: Int,
    resolution: String,
    hd: Boolean,
    onBitrate: (Int) -> Unit,
    onResolution: (String) -> Unit,
    onHd: (Boolean) -> Unit,
) {
    when (format) {
        "mp3", "m4a" -> {
            LabeledDropdown(
                label = DownloaderApp.str(R.string.quality_bitrate),
                value = bitrate,
                options = BitrateOptions,
                optionText = {
                    if (it == 320) {
                        DownloaderApp.str(R.string.quality_bitrate_best, it)
                    } else {
                        DownloaderApp.str(R.string.quality_bitrate_value, it)
                    }
                },
                onSelect = onBitrate,
            )
        }

        "mp4" -> {
            val heights = meta.formats.video
                .mapNotNull { it.height }
                .filter { it in 144..2160 }
                .distinct()
                .sortedDescending()
                .ifEmpty { listOf(1080, 720, 480, 360) }

            Column {
                LabeledDropdown(
                    label = DownloaderApp.str(R.string.quality_resolution),
                    value = resolution,
                    options = listOf("auto") + heights.map { it.toString() },
                    optionText = {
                        if (it == "auto") {
                            DownloaderApp.str(R.string.quality_resolution_auto)
                        } else {
                            DownloaderApp.str(R.string.quality_resolution_value, it.toIntOrNull() ?: it)
                        }
                    },
                    onSelect = onResolution,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DownloaderApp.str(R.string.quality_hd_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = DownloaderApp.str(R.string.quality_hd_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = hd && resolution == "auto",
                        onCheckedChange = onHd,
                        enabled = resolution == "auto",
                    )
                }
            }
        }

        "photo" -> {
            Text(
                text = DownloaderApp.str(R.string.quality_photo_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> LabeledDropdown(
    label: String,
    value: T,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = optionText(value),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        leadingIcon = if (option == value) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistSection(
    playlist: PlaylistInfo,
    selected: Set<Int>,
    zipMode: Boolean,
    format: String,
    bitrate: Int,
    resolution: String,
    hd: Boolean,
    totalEstimateBytes: Long?,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    onZipChange: (Boolean) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DownloaderColors.SurfaceElevated),
        modifier = Modifier.animateContentSize(tween(240)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title ?: DownloaderApp.str(R.string.playlist_default_title),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = DownloaderApp.str(
                            R.string.playlist_summary,
                            playlist.itemCount,
                            Formatters.duration(playlist.totalDuration),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = DownloaderApp.str(R.string.playlist_selected_count, selected.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) {
                    Text(DownloaderApp.str(R.string.playlist_select_all))
                }
                TextButton(onClick = onUnselectAll) {
                    Text(DownloaderApp.str(R.string.playlist_unselect_all))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = DownloaderApp.str(R.string.playlist_zip),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Switch(checked = zipMode, onCheckedChange = onZipChange)
            }

            if (totalEstimateBytes != null && totalEstimateBytes > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DownloaderApp.str(R.string.estimate_total, Formatters.bytes(totalEstimateBytes)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
            ) {
                items(playlist.items, key = { it.index }) { item ->
                    PlaylistRow(
                        item = item,
                        checked = item.index in selected,
                        estimateBytes = SizeEstimator.estimateBytes(
                            format = format,
                            bitrate = bitrate,
                            resolution = resolution,
                            hd = hd,
                            durationSeconds = item.duration,
                        ),
                        onToggle = { onToggle(item.index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    item: PlaylistItem,
    checked: Boolean,
    estimateBytes: Long?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = "#${item.index}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.duration != null && item.duration > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = Formatters.duration(item.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (estimateBytes != null && estimateBytes > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = DownloaderApp.str(R.string.estimate_short, Formatters.bytes(estimateBytes)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrimaryDownloadButton(
    enabled: Boolean,
    hint: String?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Button(
            onClick = onClick,
            enabled = enabled && !loading,
            shape = RoundedCornerShape(DownloaderRadius.card),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .pressScale(scaleDown = 0.97f),
        ) {
            if (loading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = DownloaderApp.str(R.string.action_download_now),
                    style = DownloaderText.button,
                )
            }
        }
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = DownloaderColors.Danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
