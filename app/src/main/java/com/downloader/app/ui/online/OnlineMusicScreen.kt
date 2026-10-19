package com.downloader.app.ui.online

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.downloader.app.ui.components.EmptyStateBox
import com.downloader.app.ui.components.SectionHeader
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing

/**
 * Online Music: pencarian memakai mesin yang sudah ada.
 *
 * Bagian "Kategori & Trending" sengaja diberi keterangan jujur karena sumber
 * kontennya belum dikonfigurasi — bukan diisi data contoh.
 */
@Composable
fun OnlineMusicScreen(
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineMusicViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        keyboard?.hide()
        viewModel.search(query)
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
        item {
            Column {
                Text(
                    text = DownloaderApp.str(R.string.online_music_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DownloaderApp.str(R.string.online_music_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DownloaderColors.TextSecondary,
                )
            }
        }

        item {
            SearchBar(
                value = query,
                onValueChange = { query = it },
                onSubmit = ::submit,
            )
        }

        when (val current = state) {
            is OnlineMusicState.Idle -> {
                item {
                    EmptyStateBox(
                        icon = Icons.Rounded.Public,
                        title = DownloaderApp.str(R.string.online_music_idle_title),
                        description = DownloaderApp.str(R.string.online_music_idle_desc),
                    )
                }
                item { UnavailableNotice() }
            }

            is OnlineMusicState.Searching -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = DownloaderColors.TextSecondary,
                        )
                    }
                }
            }

            is OnlineMusicState.Failed -> {
                item {
                    EmptyStateBox(
                        icon = Icons.Rounded.ErrorOutline,
                        title = DownloaderApp.str(R.string.online_music_error_title),
                        description = current.message,
                    )
                }
                item {
                    Surface(
                        onClick = ::submit,
                        shape = RoundedCornerShape(DownloaderRadius.pill),
                        color = DownloaderColors.Primary,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = DownloaderApp.str(R.string.online_music_retry),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            is OnlineMusicState.Results -> {
                if (current.tracks.isEmpty()) {
                    item {
                        EmptyStateBox(
                            icon = Icons.Rounded.Search,
                            title = DownloaderApp.str(R.string.online_music_empty_title),
                            description = DownloaderApp.str(
                                R.string.online_music_empty_desc,
                                current.query,
                            ),
                        )
                    }
                } else {
                    item {
                        SectionHeader(
                            title = DownloaderApp.str(R.string.online_music_results, current.query),
                        )
                    }
                    item {
                        Text(
                            text = DownloaderApp.str(
                                R.string.online_music_results_count,
                                current.tracks.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = DownloaderColors.TextFaint,
                        )
                    }
                    items(current.tracks, key = { it.id }) { track ->
                        OnlineTrackCard(track = track, onDownload = { onDownload(track.url) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.pill),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = DownloaderColors.TextFaint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = DownloaderApp.str(R.string.online_music_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DownloaderColors.TextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = DownloaderColors.TextPrimary,
                    ),
                    cursorBrush = SolidColor(DownloaderColors.Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = DownloaderColors.TextFaint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(DownloaderRadius.pill),
                    color = DownloaderColors.Primary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ) {
                    Text(
                        text = DownloaderApp.str(R.string.online_music_search),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineTrackCard(track: OnlineTrack, onDownload: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = DownloaderColors.SurfaceElevated,
        border = BorderStroke(1.dp, DownloaderColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DownloaderSpacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(DownloaderRadius.sm))
                    .background(DownloaderColors.SurfaceVariant),
            )
            Spacer(Modifier.width(DownloaderSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.uploader ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = DownloaderColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (track.durationSeconds != null) {
                        Text(
                            text = "  •  " + Formatters.duration(track.durationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = DownloaderColors.TextFaint,
                        )
                    }
                }
            }
            Spacer(Modifier.width(DownloaderSpacing.sm))
            Surface(
                onClick = onDownload,
                shape = RoundedCornerShape(DownloaderRadius.pill),
                color = DownloaderColors.PrimarySoft,
                contentColor = DownloaderColors.Cyan,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = DownloaderApp.str(R.string.online_music_result_action),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailableNotice() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(DownloaderSpacing.lg)) {
            Text(
                text = DownloaderApp.str(R.string.online_music_sections_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = DownloaderApp.str(R.string.online_music_sections_desc),
                style = MaterialTheme.typography.bodySmall,
                color = DownloaderColors.TextSecondary,
            )
        }
    }
}
