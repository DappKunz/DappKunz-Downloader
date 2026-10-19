package com.downloader.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.downloader.app.ui.theme.ArtworkTones
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

/**
 * Inisial nama untuk placeholder artwork, mengikuti pola desain
 * ("Neon Lights" → "NE", "M83" → "M83", "Weekend_Recap.mp4" → "WR").
 */
fun artworkInitials(name: String?): String {
    val cleaned = name?.substringBeforeLast('.')?.trim().orEmpty()
    val parts = cleaned.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    if (parts.size == 1) {
        val single = parts[0]
        return if (single.length <= 3) single.uppercase() else single.take(2).uppercase()
    }
    return (parts[0].take(1) + parts[1].take(1)).uppercase()
}

/**
 * Label pendek pada artwork untuk jenis berkas (desain memakai "MP4", "AU").
 * Dipakai bersama oleh kartu riwayat dan baris "Baru diunduh" di Beranda.
 */
fun formatTagLabel(format: String): String = when (format.lowercase()) {
    "mp4", "video" -> "MP4"
    "mp3" -> "MP3"
    "m4a" -> "M4A"
    "photo", "image" -> "IMG"
    else -> format.uppercase().take(4)
}

/**
 * Artwork hasil desain: kotak warna dasar + lingkaran aksen (kanan-atas) +
 * lingkaran gelap (kiri-bawah), dengan label/ikon opsional.
 *
 * Pola ini dipakai untuk cover lagu, album, playlist, thumbnail unduhan, dan
 * artwork Now Playing. Tone dipilih stabil dari [seed] supaya berkas/lagu yang
 * sama selalu tampil dengan warna yang sama.
 */
@Composable
fun ArtworkTile(
    seed: String?,
    modifier: Modifier = Modifier,
    corner: Dp = DownloaderRadius.artwork,
    label: String? = null,
    labelStyle: TextStyle = DownloaderText.status,
    labelColor: Color = DownloaderColors.TextPrimary,
    labelPadding: Dp = 4.dp,
    icon: ImageVector? = null,
    iconSize: Dp = 20.dp,
    iconTint: Color = DownloaderColors.TextPrimary,
    contentDescription: String? = null,
) {
    val tone = remember(seed) { ArtworkTones.forSeed(seed) }

    val base = modifier
        .clip(RoundedCornerShape(corner))
        .drawBehind {
            drawRect(color = tone.base)
            val min = size.minDimension
            drawCircle(
                color = tone.accent.copy(alpha = 0.65f),
                radius = min * 0.34f,
                center = Offset(size.width * 0.78f, size.height * 0.16f),
            )
            drawCircle(
                color = DownloaderColors.Background.copy(alpha = 0.72f),
                radius = min * 0.36f,
                center = Offset(size.width * 0.12f, size.height * 0.95f),
            )
        }

    val described = if (contentDescription != null) {
        base.semantics { this.contentDescription = contentDescription }
    } else {
        base
    }

    Box(
        modifier = described,
        contentAlignment = if (icon != null) Alignment.Center else Alignment.BottomCenter,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(bottom = labelPadding),
            )
        }
    }
}
