package com.downloader.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
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
import com.downloader.app.ui.components.breathing
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
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
    var showMore by remember { mutableStateOf(false) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(0, offsetY = 8),
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                Surface(
                    onClick = { showQueue = true },
                    shape = RoundedCornerShape(11.dp),
                    color = DownloaderColors.Surface,
                    border = BorderStroke(1.dp, DownloaderColors.Border),
                    modifier = Modifier
                        .size(width = 38.dp, height = 34.dp)
                        .pressScale(scaleDown = 0.9f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = DownloaderApp.str(R.string.player_open_queue),
                            tint = DownloaderColors.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (track == null) {
                NothingPlaying()
                Spacer(Modifier.height(16.dp))
                return@Column
            }

            val current = track ?: return@Column

            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .aspectRatio(1f)
                    .entrance(1, offsetY = 18),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .background(
                            color = DownloaderColors.Primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(34.dp),
                        )
                        .breathing(scale = 1.035f),
                )
                TrackArtwork(
                    track = current,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    corner = DownloaderRadius.artworkLg,
                    iconSize = 92.dp,
                )
            }

            Spacer(Modifier.height(16.dp))

            val favorite = current.id in favorites
            val favoriteColor by animateColorAsState(
                targetValue = if (favorite) DownloaderColors.Primary else DownloaderColors.TextMuted,
                animationSpec = tween(220),
                label = "nowFavoriteColor",
            )
            val favoriteScale by animateFloatAsState(
                targetValue = if (favorite) 1.14f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 640f),
                label = "nowFavoriteScale",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2, offsetY = 12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = DownloaderText.screenTitleLg,
                        color = DownloaderColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(current.artist, current.album).joinToString(" • ")
                            .ifEmpty { DownloaderApp.str(R.string.player_unknown_artist) },
                        style = DownloaderText.rowTitle,
                        color = DownloaderColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    onClick = { PrefsRepository.toggleFavoriteTrack(current.id) },
                    shape = CircleShape,
                    color = DownloaderColors.Surface,
                    border = BorderStroke(1.dp, DownloaderColors.Border),
                    modifier = Modifier
                        .size(36.dp)
                        .pressScale(scaleDown = 0.86f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (favorite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = DownloaderApp.str(
                                if (favorite) {
                                    R.string.player_favorite_remove
                                } else {
                                    R.string.player_favorite_add
                                },
                            ),
                            tint = favoriteColor,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(favoriteScale),
                        )
                    }
                }
            }

            if (loading) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = DownloaderApp.str(R.string.player_preparing),
                        style = DownloaderText.metaSm,
                        color = DownloaderColors.TextSecondary,
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

            val shuffleColor by animateColorAsState(
                targetValue = if (shuffle) DownloaderColors.Primary else DownloaderColors.TextMuted,
                animationSpec = tween(220),
                label = "shuffleTint",
            )
            val repeatColor by animateColorAsState(
                targetValue = if (repeatMode != MusicController.REPEAT_OFF) {
                    DownloaderColors.Primary
                } else {
                    DownloaderColors.TextMuted
                },
                animationSpec = tween(220),
                label = "repeatTint",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(3, offsetY = 12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = MusicController::toggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = DownloaderApp.str(R.string.player_shuffle),
                        tint = shuffleColor,
                        modifier = Modifier.size(22.dp),
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
                    modifier = Modifier.size(74.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(
                                color = DownloaderColors.Primary.copy(alpha = 0.16f),
                                shape = CircleShape,
                            )
                            .breathing(active = playing, scale = 1.08f),
                    )
                    Surface(
                        onClick = { MusicController.toggle(context) },
                        shape = CircleShape,
                        color = DownloaderColors.Primary,
                        contentColor = DownloaderColors.OnPrimary,
                        modifier = Modifier
                            .size(66.dp)
                            .pressScale(scaleDown = 0.92f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = playing,
                                transitionSpec = {
                                    (fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.6f)) togetherWith
                                        (fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.7f))
                                },
                                label = "playIcon",
                            ) { isPlaying ->
                                Icon(
                                    imageVector = if (isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = DownloaderApp.str(
                                        if (isPlaying) R.string.player_pause else R.string.player_play,
                                    ),
                                    tint = DownloaderColors.OnPrimary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
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
                        tint = repeatColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(4, offsetY = 12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                PlayerActionPill(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = DownloaderApp.str(R.string.player_open_queue),
                    onClick = { showQueue = true },
                    modifier = Modifier.weight(1f),
                )
                PlayerActionPill(
                    icon = Icons.Rounded.Equalizer,
                    label = DownloaderApp.str(R.string.eq_title),
                    onClick = { showEqualizer = true },
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f)) {
                    PlayerActionPill(
                        icon = Icons.Rounded.MoreHoriz,
                        label = DownloaderApp.str(R.string.player_more),
                        onClick = { showMore = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = showMore,
                        onDismissRequest = { showMore = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(DownloaderApp.str(R.string.action_close)) },
                            leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                            onClick = {
                                showMore = false
                                onClose()
                            },
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(5, offsetY = 8),
                    shape = RoundedCornerShape(13.dp),
                    color = DownloaderColors.DangerSoft,
                    border = BorderStroke(1.dp, DownloaderColors.Danger.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error ?: DownloaderApp.str(R.string.player_error_generic),
                            style = DownloaderText.meta,
                            color = DownloaderColors.Danger,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = MusicController::dismissError, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = DownloaderApp.str(R.string.action_close),
                                tint = DownloaderColors.Danger,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        AnimatedVisibility(
            visible = showEqualizer,
            enter = slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(180)),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DownloaderColors.Background,
            ) {
                EqualizerScreen(onClose = { showEqualizer = false })
            }
        }

        AnimatedVisibility(
            visible = showQueue,
            enter = slideInHorizontally(tween(320)) { it / 2 } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(260)) { it / 2 } + fadeOut(tween(180)),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DownloaderColors.Background,
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
            .padding(vertical = 48.dp)
            .entrance(1, offsetY = 12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    color = DownloaderColors.PrimarySoft,
                    shape = CircleShape,
                )
                .breathing(scale = 1.05f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = DownloaderColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = DownloaderApp.str(R.string.player_empty_title),
            style = DownloaderText.sectionTitle,
            color = DownloaderColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlayerActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier
            .height(38.dp)
            .pressScale(scaleDown = 0.94f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DownloaderColors.TextPrimary,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .entrance(0, offsetY = 8),
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
                    style = DownloaderText.screenTitle,
                    color = DownloaderColors.TextPrimary,
                )
                Text(
                    text = DownloaderApp.str(R.string.player_queue_count, queue.size),
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                )
            }
        }

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .entrance(1, offsetY = 10),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = DownloaderApp.str(R.string.player_queue_empty),
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                    val isCurrent = item.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) DownloaderColors.PrimarySoft else Color.Transparent,
                            )
                            .clickable { onSelect(index) }
                            .pressScale(scaleDown = 0.985f)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .entrance(index.coerceAtMost(8), offsetY = 8)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = DownloaderText.metaSm,
                            color = if (isCurrent) {
                                DownloaderColors.Primary
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
                                style = DownloaderText.rowTitleSm,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) {
                                    DownloaderColors.PrimaryLight
                                } else {
                                    DownloaderColors.TextPrimary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.artist
                                    ?: DownloaderApp.str(R.string.player_unknown_artist),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
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
