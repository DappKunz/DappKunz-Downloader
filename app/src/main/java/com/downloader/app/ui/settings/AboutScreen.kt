package com.downloader.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.update.UpdateChecker
import com.downloader.app.ui.components.breathing
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText

/**
 * Layar "Tentang Aplikasi" — mengikuti frame desain `7:858`.
 *
 * Susunan mengikuti frame `7:858`: header, logo 88, nama aplikasi + versi,
 * kartu Developer (avatar 48 + tautan profil GitHub), kartu Maintainer (dua
 * baris beravatar), kartu Komponen & Lisensi, lalu tombol GitHub Repository.
 */
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) { appVersionName(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Text(
                text = DownloaderApp.str(R.string.about_title),
                style = DownloaderText.screenTitleSm,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DownloaderSpacing.screen)
                .padding(bottom = DownloaderSpacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DownloaderSpacing.xl))

            Image(
                painter = painterResource(R.drawable.fluxen_logo),
                contentDescription = DownloaderApp.str(R.string.about_logo_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(DownloaderDimens.aboutLogo)
                    .entrance(0, offsetY = 14)
                    .breathing(),
            )

            Spacer(Modifier.height(DownloaderSpacing.xxl))

            Text(
                text = DownloaderApp.str(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.entrance(1, offsetY = 10),
            )
            Spacer(Modifier.height(DownloaderSpacing.xxs))
            Text(
                text = DownloaderApp.str(R.string.about_version_short, versionName),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
                modifier = Modifier.entrance(1, offsetY = 10),
            )

            Spacer(Modifier.height(DownloaderSpacing.xxxl))

            DeveloperCard(
                onOpenProfile = { openExternally(context, GITHUB_DEVELOPER) },
                modifier = Modifier.entrance(2),
            )
            Spacer(Modifier.height(DownloaderSpacing.lg))
            MaintainersCard(
                onOpenDappKunz = { openExternally(context, GITHUB_DEVELOPER) },
                onOpenN4zzuu = { openExternally(context, GITHUB_MAINTAINER) },
                modifier = Modifier.entrance(3),
            )
            Spacer(Modifier.height(DownloaderSpacing.lg))
            ComponentsCard(
                onOpenChangelog = { openExternally(context, UpdateChecker.RELEASES_PAGE) },
                modifier = Modifier.entrance(4),
            )
            Spacer(Modifier.height(DownloaderSpacing.lg))
            RepositoryButton(
                onClick = { openExternally(context, UpdateChecker.REPO_PAGE) },
                modifier = Modifier.entrance(5),
            )
        }
    }
}

/**
 * Kartu Developer (desain: 353×87, avatar bulat 48, teks role + nama).
 *
 * Avatar memakai foto profil GitHub developer; menekan kartu membuka profilnya.
 */
@Composable
private fun DeveloperCard(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = DownloaderApp.str(R.string.about_developer_value)
    AboutCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = DownloaderApp.str(R.string.about_open_profile, name),
                    onClick = onOpenProfile,
                )
                .pressScale(scaleDown = 0.985f)
                .padding(horizontal = 14.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.about_avatar_dappkunz),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(DownloaderDimens.avatarLg)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(DownloaderSpacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DownloaderApp.str(R.string.about_developer_label),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.sm))
                Text(
                    text = name,
                    style = DownloaderText.nameLg,
                    color = DownloaderColors.TextPrimary,
                )
            }
            ProfileChevron()
        }
    }
}

/**
 * Kartu Maintainer: satu baris per akun, avatar bulat 36 + nama, dapat ditekan
 * untuk membuka profil GitHub masing-masing.
 */
@Composable
private fun MaintainersCard(
    onOpenDappKunz: () -> Unit,
    onOpenN4zzuu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AboutCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = DownloaderApp.str(R.string.about_maintainers_label),
                style = DownloaderText.meta,
                color = DownloaderColors.TextSecondary,
            )
            Spacer(Modifier.height(DownloaderSpacing.sm))
            MaintainerRow(
                avatarRes = R.drawable.about_avatar_dappkunz,
                nameRes = R.string.about_maintainer_1_name,
                onClick = onOpenDappKunz,
            )
            MaintainerRow(
                avatarRes = R.drawable.about_avatar_n4zzuu,
                nameRes = R.string.about_maintainer_2_name,
                onClick = onOpenN4zzuu,
            )
        }
    }
}

@Composable
private fun MaintainerRow(avatarRes: Int, nameRes: Int, onClick: () -> Unit) {
    val name = DownloaderApp.str(nameRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DownloaderRadius.row))
            .clickable(
                onClickLabel = DownloaderApp.str(R.string.about_open_profile, name),
                onClick = onClick,
            )
            .pressScale(scaleDown = 0.98f)
            .height(DownloaderDimens.rowHeight)
            .padding(horizontal = DownloaderSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(DownloaderDimens.artworkSm)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(DownloaderSpacing.md))
        Text(
            text = name,
            style = DownloaderText.rowTitle,
            color = DownloaderColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        ProfileChevron()
    }
}

@Composable
private fun ProfileChevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = DownloaderColors.TextMuted,
        modifier = Modifier.size(20.dp),
    )
}

/** Kartu Komponen & Lisensi (desain: 353×111) + tautan "Source & changelog". */
@Composable
private fun ComponentsCard(
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AboutCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = DownloaderApp.str(R.string.about_components_title),
                style = DownloaderText.cardTitle,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(DownloaderSpacing.sm))
            Text(
                text = DownloaderApp.str(R.string.about_components_list),
                style = DownloaderText.meta,
                color = DownloaderColors.TextSecondary,
            )
            Spacer(Modifier.height(DownloaderSpacing.xxs))
            Text(
                text = DownloaderApp.str(R.string.about_licenses_note),
                style = DownloaderText.meta,
                color = DownloaderColors.TextSecondary,
            )
            Spacer(Modifier.height(DownloaderSpacing.sm))
            Text(
                text = DownloaderApp.str(R.string.about_source_changelog),
                style = DownloaderText.sectionLink,
                color = DownloaderColors.Primary,
                modifier = Modifier
                    .clickable(onClick = onOpenChangelog)
                    .padding(vertical = DownloaderSpacing.xs, horizontal = DownloaderSpacing.xxs),
            )
        }
    }
}

/** Tombol GitHub Repository (desain: 353×48, radius 15, biru lembut + border). */
@Composable
private fun RepositoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DownloaderRadius.card),
        color = DownloaderColors.PrimarySoft,
        border = BorderStroke(1.dp, DownloaderColors.BorderSoftBlue),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(scaleDown = 0.97f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DownloaderDimens.buttonHeightLg)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Language,
                contentDescription = null,
                tint = DownloaderColors.TextOnSoft,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(DownloaderSpacing.md))
            Text(
                text = DownloaderApp.str(R.string.about_github_repo),
                style = DownloaderText.rowTitle,
                color = DownloaderColors.TextOnSoft,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = DownloaderColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Kartu dasar gaya desain: radius 16, isian `#151821`, border `#2B3242`. */
@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.cardLg),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        content()
    }
}

/** Profil GitHub developer & maintainer (dibuka dari kartu Tentang). */
private const val GITHUB_DEVELOPER = "https://github.com/DappKunz"
private const val GITHUB_MAINTAINER = "https://github.com/n4zzuu"

private fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
} catch (_: PackageManager.NameNotFoundException) {
    "-"
}

private fun appSdkRange(context: Context): String {
    val info = context.applicationInfo
    return DownloaderApp.str(R.string.about_sdk_range, info.minSdkVersion, info.targetSdkVersion)
}

/** Buka tautan https tetap (repo/changelog) di peramban. */
private fun openExternally(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
