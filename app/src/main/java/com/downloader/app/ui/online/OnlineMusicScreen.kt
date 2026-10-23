package com.downloader.app.ui.online

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.components.ArtworkTile
import com.downloader.app.ui.components.DownloaderChip
import com.downloader.app.ui.components.EmptyStateBox
import com.downloader.app.ui.components.breathing
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText

/**
 * Online Music discovery layout from Figma's "Screen / 05".
 *
 * The hero, Trending Now rows and Made For You cards are real search shortcuts,
 * not sample catalog data: every card submits a query to the existing online
 * search engine and every result keeps the real download action.
 */
@Composable
fun OnlineMusicScreen(
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineMusicViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGenre by rememberSaveable { mutableStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun runSearch(text: String, genre: Int = selectedGenre) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        query = trimmed
        selectedGenre = genre
        keyboard?.hide()
        viewModel.search(trimmed)
    }

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
        item(key = "online-header") {
            Column(modifier = Modifier.entrance(0, offsetY = 10)) {
                Text(
                    text = DownloaderApp.str(R.string.online_music_title),
                    style = DownloaderText.screenTitle,
                    color = DownloaderColors.TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = DownloaderApp.str(R.string.online_music_subtitle),
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                )
            }
        }

        item(key = "online-search") {
            SearchBar(
                value = query,
                onValueChange = { query = it },
                onSubmit = { runSearch(query) },
                modifier = Modifier.entrance(1, offsetY = 10),
            )
        }

        item(key = "online-genres") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .entrance(2, offsetY = 10),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GenreShortcuts.forEachIndexed { index, genre ->
                    DownloaderChip(
                        label = DownloaderApp.str(genre.labelRes),
                        selected = selectedGenre == index,
                        onClick = {
                            selectedGenre = index
                            runSearch(DownloaderApp.str(genre.queryRes), index)
                        },
                    )
                }
            }
        }

        when (val current = state) {
            is OnlineMusicState.Idle -> {
                item(key = "online-hero") {
                    FeaturedPlaylist(
                        onPlay = { runSearch("trending music", 0) },
                        modifier = Modifier.entrance(3, offsetY = 14),
                    )
                }

                item(key = "online-trending-header") {
                    SectionLinkHeader(
                        title = DownloaderApp.str(R.string.online_music_trending),
                        onAction = { runSearch("trending music", 0) },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .entrance(4, offsetY = 8),
                    )
                }

                itemsIndexed(
                    TrendingShortcuts,
                    key = { _, item -> "trend-${item.seed}" },
                ) { index, shortcut ->
                    ShortcutRow(
                        title = DownloaderApp.str(shortcut.titleRes),
                        seed = shortcut.seed,
                        onClick = { runSearch(shortcut.query, 0) },
                        modifier = Modifier
                            .entrance(index.coerceAtMost(6) + 5, offsetY = 10)
                            .animateItem(),
                    )
                }

                item(key = "online-made-header") {
                    SectionLinkHeader(
                        title = DownloaderApp.str(R.string.online_music_made_for_you),
                        onAction = { runSearch("playlist music", 0) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .entrance(7, offsetY = 8),
                    )
                }

                item(key = "online-made") {
                    MadeForYouRow(
                        onPick = { runSearch(it, 0) },
                        modifier = Modifier.entrance(8, offsetY = 12),
                    )
                }
            }

            is OnlineMusicState.Searching -> {
                item(key = "online-searching") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 34.dp)
                            .entrance(3),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = DownloaderColors.Primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = DownloaderApp.str(R.string.online_music_searching),
                            style = DownloaderText.body,
                            color = DownloaderColors.TextSecondary,
                        )
                    }
                }
            }

            is OnlineMusicState.Failed -> {
                item(key = "online-error") {
                    EmptyStateBox(
                        icon = Icons.Rounded.ErrorOutline,
                        title = DownloaderApp.str(R.string.online_music_error_title),
                        description = current.message,
                        modifier = Modifier.entrance(3),
                    )
                }
                item(key = "online-retry") {
                    Surface(
                        onClick = { runSearch(query) },
                        shape = RoundedCornerShape(15.dp),
                        color = DownloaderColors.Primary,
                        contentColor = DownloaderColors.OnPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .pressScale(scaleDown = 0.97f)
                            .entrance(4),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = DownloaderApp.str(R.string.online_music_retry),
                                style = DownloaderText.button,
                            )
                        }
                    }
                }
            }

            is OnlineMusicState.Results -> {
                if (current.tracks.isEmpty()) {
                    item(key = "online-empty") {
                        EmptyStateBox(
                            icon = Icons.Rounded.Search,
                            title = DownloaderApp.str(R.string.online_music_empty_title),
                            description = DownloaderApp.str(
                                R.string.online_music_empty_desc,
                                current.query,
                            ),
                            modifier = Modifier.entrance(3),
                        )
                    }
                } else {
                    item(key = "online-results-header") {
                        Column(modifier = Modifier.entrance(3, offsetY = 8)) {
                            Text(
                                text = DownloaderApp.str(R.string.online_music_results, current.query),
                                style = DownloaderText.sectionTitle,
                                color = DownloaderColors.TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = DownloaderApp.str(
                                    R.string.online_music_results_count,
                                    current.tracks.size,
                                ),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextMuted,
                            )
                        }
                    }
                    itemsIndexed(current.tracks, key = { _, track -> track.id }) { index, track ->
                        OnlineTrackCard(
                            track = track,
                            onDownload = { onDownload(track.url) },
                            modifier = Modifier
                                .entrance(index.coerceAtMost(8) + 4, offsetY = 10)
                                .animateItem(),
                        )
                    }
                }
            }
        }
    }
}

private data class GenreShortcut(
    val labelRes: Int,
    val queryRes: Int,
)

private data class Shortcut(
    val titleRes: Int,
    val seed: String,
    val query: String,
)

private val GenreShortcuts = listOf(
    GenreShortcut(R.string.online_music_chip_for_you, R.string.online_music_shortcut_pop),
    GenreShortcut(R.string.online_music_chip_pop, R.string.online_music_shortcut_pop),
    GenreShortcut(R.string.online_music_chip_lofi, R.string.online_music_shortcut_lofi),
    GenreShortcut(R.string.online_music_chip_rock, R.string.online_music_shortcut_rock),
    GenreShortcut(R.string.online_music_chip_jazz, R.string.online_music_shortcut_jazz),
)

private val TrendingShortcuts = listOf(
    Shortcut(R.string.online_music_shortcut_pop, "pop", "pop hits"),
    Shortcut(R.string.online_music_shortcut_lofi, "lofi", "lofi chill"),
    Shortcut(R.string.online_music_shortcut_rock, "rock", "classic rock"),
    Shortcut(R.string.online_music_shortcut_jazz, "jazz", "jazz night"),
)

private val PlaylistShortcuts = listOf(
    Shortcut(R.string.online_music_playlist_top, "top", "top hits music"),
    Shortcut(R.string.online_music_playlist_chill, "chill", "chill vibes music"),
    Shortcut(R.string.online_music_playlist_acoustic, "acoustic", "acoustic songs"),
)

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DownloaderRadius.field)
    val borderColor by animateColorAsState(
        targetValue = if (focused) DownloaderColors.Primary else DownloaderColors.Border,
        animationSpec = tween(220),
        label = "onlineSearchBorder",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(DownloaderColors.Background)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = DownloaderColors.TextMuted,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = DownloaderApp.str(R.string.online_music_hint),
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = DownloaderText.rowTitleSm.copy(color = DownloaderColors.TextPrimary),
                cursorBrush = SolidColor(DownloaderColors.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(26.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = DownloaderApp.str(R.string.action_clear_link),
                    tint = DownloaderColors.TextMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(
                onClick = onSubmit,
                modifier = Modifier
                    .size(30.dp)
                    .pressScale(scaleDown = 0.9f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = DownloaderApp.str(R.string.online_music_search),
                    tint = DownloaderColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FeaturedPlaylist(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(149.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DownloaderColors.PrimarySoft),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 52.dp, y = (-44).dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(DownloaderColors.Primary.copy(alpha = 0.34f))
                .breathing(scale = 1.05f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = 54.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(DownloaderColors.Cyan.copy(alpha = 0.18f))
                .breathing(scale = 1.08f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 14.dp, end = 70.dp, bottom = 14.dp),
        ) {
            Text(
                text = DownloaderApp.str(R.string.online_music_tag),
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextOnSoft,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = DownloaderApp.str(R.string.online_music_hero_title),
                style = DownloaderText.screenTitle,
                color = DownloaderColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = DownloaderApp.str(R.string.online_music_hero_meta),
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            onClick = onPlay,
            shape = CircleShape,
            color = DownloaderColors.Primary,
            contentColor = DownloaderColors.OnPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(46.dp)
                .pressScale(scaleDown = 0.9f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = DownloaderApp.str(R.string.online_music_hero_action),
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLinkHeader(
    title: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = DownloaderText.sectionTitle,
            color = DownloaderColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = DownloaderApp.str(R.string.history_see_all),
            style = DownloaderText.metaSm,
            color = DownloaderColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .pressScale(scaleDown = 0.94f)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    seed: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DownloaderColors.Surface,
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .pressScale(scaleDown = 0.985f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(41.dp)) {
                ArtworkTile(
                    seed = seed,
                    modifier = Modifier.matchParentSize(),
                    corner = DownloaderRadius.artwork,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DownloaderText.rowTitleSm,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = DownloaderApp.str(R.string.online_music_shortcut_desc),
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = DownloaderColors.Primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(17.dp),
            )
        }
    }
}

@Composable
private fun MadeForYouRow(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        PlaylistShortcuts.forEach { shortcut ->
            ShortcutCard(
                title = DownloaderApp.str(shortcut.titleRes),
                seed = shortcut.seed,
                onClick = { onPick(shortcut.query) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    seed: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .height(85.dp)
            .clip(shape)
            .background(DownloaderColors.Surface)
            .border(1.dp, DownloaderColors.Border, shape)
            .clickable(onClick = onClick)
            .pressScale(scaleDown = 0.95f),
    ) {
        ArtworkTile(
            seed = seed,
            modifier = Modifier.matchParentSize(),
            corner = 13.dp,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(30.dp)
                .background(Color.Black.copy(alpha = 0.58f)),
        )
        Text(
            text = title,
            style = DownloaderText.metaSm,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 7.dp, end = 7.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun OnlineTrackCard(
    track: OnlineTrack,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onDownload,
        shape = RoundedCornerShape(12.dp),
        color = DownloaderColors.Surface,
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .pressScale(scaleDown = 0.985f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(41.dp)) {
                ArtworkTile(
                    seed = track.id,
                    modifier = Modifier.matchParentSize(),
                    corner = DownloaderRadius.artwork,
                )
                if (!track.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = track.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(DownloaderRadius.artwork)),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = DownloaderText.rowTitleSm,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = buildString {
                        append(track.uploader ?: "—")
                        track.durationSeconds?.let {
                            append("  •  ")
                            append(Formatters.duration(it))
                        }
                    },
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                onClick = onDownload,
                shape = CircleShape,
                color = DownloaderColors.PrimarySoft,
                contentColor = DownloaderColors.Primary,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(32.dp)
                    .pressScale(scaleDown = 0.88f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = DownloaderApp.str(R.string.online_music_result_action),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
