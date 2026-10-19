package com.downloader.app.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.music.MusicController
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText

/**
 * Mini player mengapung (desain: 353×46–48, radius 14, fill `#171E2A`).
 *
 * Tombol putar 30px biru sesuai desain; tombol "next" dipertahankan karena
 * sudah berfungsi di versi sebelumnya (desain hanya memuat tombol putar).
 */
@Composable
fun MiniPlayer(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val track by MusicController.currentTrack.collectAsState()
    val playing by MusicController.isPlaying.collectAsState()
    val hasNext by MusicController.hasNext.collectAsState()
    val current = track ?: return

    val shape = RoundedCornerShape(DownloaderRadius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DownloaderSpacing.screen, vertical = 6.dp)
            .clip(shape)
            .background(DownloaderColors.GlassRaised)
            .border(1.dp, DownloaderColors.Border, shape)
            .clickable(onClick = onOpen)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            track = current,
            modifier = Modifier.size(36.dp),
            corner = DownloaderRadius.artwork,
            iconSize = 18.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = current.title,
                style = DownloaderText.rowTitleSm,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = current.artist ?: DownloaderApp.str(R.string.player_unknown_artist),
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
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
                tint = DownloaderColors.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = MusicController::next, enabled = hasNext, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = DownloaderApp.str(R.string.player_next),
                tint = if (hasNext) DownloaderColors.TextSecondary else DownloaderColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
