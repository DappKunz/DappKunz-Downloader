package com.downloader.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.ui.MainTab
import com.downloader.app.ui.music.BetaBadge
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText

/**
 * Bottom navigation utama: Beranda · Musik (BETA) · [Download] · Online · Pengaturan.
 *
 * Bentuk mengikuti desain "Navigation / Floating Glass": panel mengapung
 * (inset 12, tinggi 68, radius 27, `#161D2A` 94%) dengan FAB 50 di tengah yang
 * menonjol 18 di atas tepi panel. Tombol Download bukan tab (tidak mengubah
 * [MainTab]) melainkan aksi membuka layar analisis media.
 */
@Composable
fun DownloaderBottomBar(
    current: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onNewDownload: () -> Unit,
) {
    val shape = RoundedCornerShape(DownloaderRadius.nav)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = DownloaderSpacing.navInset,
                end = DownloaderSpacing.navInset,
                bottom = 8.dp,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DownloaderDimens.navBarHeight)
                .clip(shape)
                .background(DownloaderColors.Glass.copy(alpha = 0.94f))
                .border(1.dp, DownloaderColors.Border, shape),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    icon = Icons.Rounded.Home,
                    label = DownloaderApp.str(R.string.nav_home),
                    selected = current == MainTab.HOME,
                    onClick = { onTabSelected(MainTab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    icon = Icons.Rounded.MusicNote,
                    label = DownloaderApp.str(R.string.nav_music),
                    selected = current == MainTab.MUSIC,
                    onClick = { onTabSelected(MainTab.MUSIC) },
                    badge = { BetaBadge(compact = true) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.weight(1f))
                BottomNavItem(
                    icon = Icons.Rounded.Explore,
                    label = DownloaderApp.str(R.string.nav_online_music),
                    selected = current == MainTab.ONLINE_MUSIC,
                    onClick = { onTabSelected(MainTab.ONLINE_MUSIC) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    icon = Icons.Rounded.Settings,
                    label = DownloaderApp.str(R.string.nav_settings),
                    selected = current == MainTab.SETTINGS,
                    onClick = { onTabSelected(MainTab.SETTINGS) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Label di bawah FAB (desain: baris label yang sama dengan item lain).
            Text(
                text = DownloaderApp.str(R.string.nav_download),
                style = DownloaderText.navLabel,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
            )
        }

        CenterDownloadButton(
            onClick = onNewDownload,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -DownloaderDimens.fabLift),
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
) {
    val iconTint = if (selected) DownloaderColors.Primary else DownloaderColors.TextMuted
    val labelTint = if (selected) DownloaderColors.PrimaryLight else DownloaderColors.TextMuted

    Column(
        modifier = modifier
            .height(DownloaderDimens.navItemHeight)
            .clip(RoundedCornerShape(DownloaderRadius.card))
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = DownloaderText.navLabel,
                color = labelTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge != null) {
                Spacer(Modifier.width(4.dp))
                badge()
            }
        }
    }
}

@Composable
private fun CenterDownloadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(140),
        label = "fabScale",
    )

    Box(
        modifier = modifier
            .size(DownloaderDimens.fabSize)
            .scale(scale)
            .clip(CircleShape)
            .background(DownloaderColors.Primary)
            .border(1.dp, DownloaderColors.PrimaryHalo, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = DownloaderApp.str(R.string.nav_new_download),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = DownloaderApp.str(R.string.nav_new_download),
            tint = DownloaderColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}
