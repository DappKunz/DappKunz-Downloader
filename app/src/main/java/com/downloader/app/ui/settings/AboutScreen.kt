package com.downloader.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText

/**
 * Layar "Tentang Aplikasi" — mengikuti frame desain `7:858`.
 *
 * Susunan: header, logo bulat 88, nama aplikasi + versi, kartu Developer
 * (avatar 48 dari `Resources/logo`), kartu App Build Information (Kotlin/Python +
 * versi & rentang SDK asli dari package manager), kartu Komponen & Lisensi,
 * lalu tombol GitHub Repository.
 */
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) { appVersionName(context) }
    val sdkRange = remember(context) { appSdkRange(context) }

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
                painter = painterResource(R.drawable.about_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(DownloaderDimens.aboutLogo)
                    .clip(CircleShape),
            )

            Spacer(Modifier.height(DownloaderSpacing.xxl))

            Text(
                text = DownloaderApp.str(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(DownloaderSpacing.xxs))
            Text(
                text = DownloaderApp.str(R.string.about_version_short, versionName),
                style = DownloaderText.meta,
                color = DownloaderColors.TextSecondary,
            )

            Spacer(Modifier.height(DownloaderSpacing.xxxl))

            DeveloperCard()
            Spacer(Modifier.height(DownloaderSpacing.lg))
            BuildInfoCard(versionName = versionName, sdkRange = sdkRange)
            Spacer(Modifier.height(DownloaderSpacing.lg))
            ComponentsCard(onOpenChangelog = { openExternally(context, UpdateChecker.RELEASES_PAGE) })
            Spacer(Modifier.height(DownloaderSpacing.lg))
            RepositoryButton(onClick = { openExternally(context, UpdateChecker.REPO_PAGE) })
        }
    }
}

/** Kartu Developer (desain: 353×87, avatar bulat 48, teks role + nama). */
@Composable
private fun DeveloperCard() {
    AboutCard {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.about_profile),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(DownloaderDimens.avatarLg)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(DownloaderSpacing.lg))
            Column {
                Text(
                    text = DownloaderApp.str(R.string.about_developer_label),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.sm))
                Text(
                    text = DownloaderApp.str(R.string.about_developer_value),
                    style = DownloaderText.nameLg,
                    color = DownloaderColors.TextPrimary,
                )
            }
        }
    }
}

/**
 * Kartu App Build Information (desain: 353×113).
 *
 * Kiri: judul, label bahasa, ikon Kotlin/Python. Kanan: label Versi, nama versi
 * asli, dan rentang SDK minimum–target perangkat (nilai desain "sdk28 - 16"
 * diganti data nyata aplikasi).
 */
@Composable
private fun BuildInfoCard(versionName: String, sdkRange: String) {
    AboutCard {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DownloaderApp.str(R.string.about_build_info),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.xs))
                Text(
                    text = DownloaderApp.str(R.string.about_language),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(DownloaderSpacing.xl)) {
                    LanguageItem(iconRes = R.drawable.ic_lang_kotlin, label = "Kotlin")
                    LanguageItem(iconRes = R.drawable.ic_lang_python, label = "Python")
                }
            }
            Column(modifier = Modifier.width(130.dp)) {
                Text(
                    text = DownloaderApp.str(R.string.about_version_label),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.xs))
                Text(
                    text = DownloaderApp.str(R.string.about_version_short, versionName),
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(DownloaderSpacing.xxs))
                Text(
                    text = sdkRange,
                    style = DownloaderText.meta,
                    color = DownloaderColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(iconRes: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(DownloaderDimens.langIcon),
        )
        Spacer(Modifier.height(DownloaderSpacing.xs))
        Text(
            text = label,
            style = DownloaderText.meta,
            color = DownloaderColors.TextSecondary,
        )
    }
}

/** Kartu Komponen & Lisensi (desain: 353×111) + tautan "Source & changelog". */
@Composable
private fun ComponentsCard(onOpenChangelog: () -> Unit) {
    AboutCard {
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
private fun RepositoryButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DownloaderRadius.card),
        color = DownloaderColors.PrimarySoft,
        border = BorderStroke(1.dp, DownloaderColors.BorderSoftBlue),
        modifier = Modifier.fillMaxWidth(),
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
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(DownloaderRadius.cardLg),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

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
