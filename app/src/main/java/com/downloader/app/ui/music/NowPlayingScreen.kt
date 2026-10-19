package com.downloader.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.music.MusicController
import com.downloader.app.core.music.Track
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

/**
 * Now Playing: layar penuh khusus pemutaran.
 *
 * Semua kontrol memakai satu sumber kebenaran yang sama dengan notifikasi sistem dan
 * mini player ([MusicController]); layar ini tidak menyimpan state pemutaran sendiri.
 */
@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val track by MusicController.currentTrack.collectAsState()
    val playing by MusicController.isPlaying.collectAsState()
    val loading by MusicController.isLoading.collectAsState()
    val positionMs by MusicController.positionMs.collectAsState()
    val durationMs by MusicController.durationMs.collectAsState()
    val shuffle by MusicController.shuffle.collectAsState()
    val repeatMode by MusicController.repeatMode.collectAsState()
    val hasNext by MusicController.hasNext.collectAsState()
    val hasPrevious by MusicController.hasPrevious.collectAsState()
    val error by MusicController.error.collectAsState()
    val favorites by PrefsRepository.favoriteTracks.collectAsState()
    val queue by MusicController.queue.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubMs by remember { mutableStateOf(0L) }

    val knownDuration = durationMs.coerceAtLeast(0L)
    val sliderMax = knownDuration.coerceAtLeast(1L).toFloat()
    val shownMs = if (scrubbing) scrubMs else positionMs
    val sliderValue = shownMs.coerceIn(0L, knownDuration.coerceAtLeast(1L)).toFloat()

    BackHandler {
        when {
            showEqualizer -> showEqualizer = false
            showQueue -> showQueue = false
            else -> onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        when {
                            showEqualizer -> showEqualizer = false
                            showQueue -> showQueue = false
                            else -> onClose()
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = DownloaderApp.str(R.string.action_back),
                    )
                }
                Text(
                    text = DownloaderApp.str(R.string.player_now_playing),
                    style = DownloaderText.sectionLink,
                    color = DownloaderColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                BetaBadge()
            }

            Spacer(Modifier.height(10.dp))

            if (track == null) {
                NothingPlaying()
                Spacer(Modifier.height(16.dp))
                return@Column
            }

            val current = track ?: return@Column

            TrackArtwork(
                track = current,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .aspectRatio(1f),
                corner = DownloaderRadius.artworkLg,
                iconSize = 92.dp,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = current.title,
                style = DownloaderText.screenTitleLg,
                color = DownloaderColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOfNotNull(current.artist, current.album).joinToString(" • ")
                    .ifEmpty { DownloaderApp.str(R.string.player_unknown_artist) },
                style = DownloaderText.rowTitle,
                color = DownloaderColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (loading) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = DownloaderApp.str(R.string.player_preparing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Slider(
                value = sliderValue,
                onValueChange = {
                    scrubbing = true
                    scrubMs = it.toLong()
                },
                onValueChangeFinished = {
                    MusicController.seekTo(scrubMs)
                    scrubbing = false
                },
                valueRange = 0f..sliderMax,
                enabled = knownDuration > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = DownloaderColors.Primary,
                    activeTrackColor = DownloaderColors.Primary,
                    inactiveTrackColor = DownloaderColors.Border,
                    disabledThumbColor = DownloaderColors.TextMuted,
                    disabledActiveTrackColor = DownloaderColors.Border,
                    disabledInactiveTrackColor = DownloaderColors.Border,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Formatters.durationLong(shownMs / 1000),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formatters.durationLong(knownDuration / 1000),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = MusicController::toggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = DownloaderApp.str(R.string.player_shuffle),
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else DownloaderColors.TextMuted,
                    )
                }
                IconButton(
                    onClick = MusicController::previous,
                    enabled = hasPrevious,
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = DownloaderApp.str(R.string.player_previous),
                        modifier = Modifier.size(34.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(DownloaderColors.Primary)
                        .clickable { MusicController.toggle(context) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = DownloaderApp.str(
                            if (playing) R.string.player_pause else R.string.player_play,
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = MusicController::next,
                    enabled = hasNext,
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = DownloaderApp.str(R.string.player_next),
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = MusicController::cycleRepeat) {
                    Icon(
                        if (repeatMode == MusicController.REPEAT_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = DownloaderApp.str(R.string.player_repeat),
                        tint = if (repeatMode != MusicController.REPEAT_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            DownloaderColors.TextMuted
                        },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val favorite = current.id in favorites
                ActionButton(
                    icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = DownloaderApp.str(
                        if (favorite) R.string.player_favorite_remove else R.string.player_favorite_add,
                    ),
                    tint = if (favorite) DownloaderColors.Primary else DownloaderColors.TextMuted,
                    onClick = { PrefsRepository.toggleFavoriteTrack(current.id) },
                )
                ActionButton(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = DownloaderApp.str(R.string.player_open_queue),
                    onClick = { showQueue = true },
                )
                ActionButton(
                    icon = Icons.Rounded.Equalizer,
                    label = DownloaderApp.str(R.string.eq_title),
                    onClick = { showEqualizer = true },
                )
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error ?: DownloaderApp.str(R.string.player_error_generic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = MusicController::dismissError, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = DownloaderApp.str(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        if (showEqualizer) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                EqualizerScreen(onClose = { showEqualizer = false })
            }
        }

        if (showQueue) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                QueuePanel(
                    queue = queue,
                    currentId = track?.id,
                    onClose = { showQueue = false },
                    onSelect = { index ->
                        MusicController.skipToQueueIndex(context, index)
                        showQueue = false
                    },
                    onRemove = MusicController::removeFromQueue,
                )
            }
        }
    }
}

@Composable
private fun NothingPlaying() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = DownloaderColors.TextMuted,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = DownloaderApp.str(R.string.player_empty_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QueuePanel(
    queue: List<Track>,
    currentId: String?,
    onClose: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DownloaderApp.str(R.string.player_queue_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = DownloaderApp.str(R.string.player_queue_count, queue.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = DownloaderApp.str(R.string.player_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(queue) { index, item ->
                    val isCurrent = item.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(index) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                DownloaderColors.TextMuted
                            },
                            modifier = Modifier.width(22.dp),
                        )
                        TrackArtwork(
                            track = item,
                            modifier = Modifier.size(42.dp),
                            corner = 9.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.artist
                                    ?: DownloaderApp.str(R.string.player_unknown_artist),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = DownloaderApp.str(R.string.player_queue_remove),
                                tint = DownloaderColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
