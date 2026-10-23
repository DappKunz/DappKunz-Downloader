package com.downloader.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.downloader.app.ui.theme.DownloaderColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Bottom sheet dengan scrim fade + slide-up. Dipakai oleh Tampilan dan Bahasa
 * supaya kedua overlay punya ritme animasi yang sama.
 *
 * [onDismiss] baru dipanggil setelah animasi keluar selesai, jadi pemanggil
 * tidak perlu mengatur delay sendiri.
 *
 * [footer] dipakai untuk aksi utama (mis. tombol Terapkan) yang harus tetap
 * terlihat di atas navigation bar, sementara isi sheet tetap bisa discroll.
 * Footer otomatis mendapat padding bottom sebesar navigation-bar inset.
 */
@Composable
fun AnimatedBottomSheet(
    onDismiss: () -> Unit,
    title: String,
    sheetHeight: Dp? = null,
    footer: (@Composable (close: () -> Unit) -> Unit)? = null,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closeRequested by remember { mutableStateOf(false) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val context = LocalContext.current
    val dark = DownloaderColors.isDark
    // Window dialog berada di antara status bar dan navigation bar, tetapi
    // konten Compose-nya masih diukur setinggi layar. Lift ini menggeser sheet
    // tepat ke atas navigation bar agar footer tidak tertutup.
    val systemBars = remember(context) { readSystemBarInsets(context) }
    val liftPx = systemBars.status + systemBars.navigation

    LaunchedEffect(Unit) {
        visible = true
    }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bottomSheetProgress",
    )
    val scrimProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "bottomSheetScrim",
    )
    val sheetColor by animateColorAsState(
        targetValue = DownloaderColors.AppearanceSheet,
        animationSpec = tween(durationMillis = 220),
        label = "bottomSheetColor",
    )

    val close = {
        if (!closeRequested) {
            closeRequested = true
            visible = false
        }
    }

    LaunchedEffect(closeRequested) {
        if (closeRequested) {
            delay(320)
            latestDismiss()
        }
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Biarkan Composable mengikuti area window yang sudah dikurangi
            // system bar. Dengan false, Compose mengukur setinggi layar penuh
            // sementara window dialog hanya setinggi area aman, sehingga sheet
            // dan tombol footer terpotong di belakang navigation bar.
            decorFitsSystemWindows = true,
        ),
    ) {
        val view = LocalView.current
        LaunchedEffect(dark) {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: return@LaunchedEffect
            window.setDimAmount(0f)
            // Dialog Compose punya window sendiri. Window ini sudah berada di
            // antara status bar dan navigation bar; warnai nav bar dengan warna
            // sheet supaya transisinya menyatu, lalu atur ikonnya agar tetap
            // terbaca di mode terang maupun gelap.
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = DownloaderColors.AppearanceSheet.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }

        val density = LocalDensity.current
        val bottomInset = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        val hiddenOffsetPx = with(density) {
            ((sheetHeight ?: 600.dp) + bottomInset).toPx()
        }
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        DownloaderColors.AppearanceScrim.copy(
                            alpha = DownloaderColors.AppearanceScrim.alpha * scrimProgress,
                        ),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = close,
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(
                        if (sheetHeight != null) {
                            Modifier.height(sheetHeight + bottomInset)
                        } else {
                            Modifier.wrapContentHeight()
                        },
                    )
                    // Offset (bukan graphicsLayer) supaya perubahan progress
                    // selalu dihitung ulang pada fase layout, termasuk frame
                    // terakhir animasi.
                    .offset {
                        val slide = (1f - progress) * (hiddenOffsetPx + liftPx)
                        IntOffset(
                            x = 0,
                            y = (-liftPx + slide).roundToInt(),
                        )
                    }
                    .alpha(progress)
                    .clip(shape)
                    .background(sheetColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .semantics { paneTitle = title },
            ) {
                if (footer == null) {
                    Column(
                        modifier = Modifier
                            .then(if (sheetHeight != null) Modifier.weight(1f) else Modifier)
                            .padding(bottom = bottomInset)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        content(close)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .then(if (sheetHeight != null) Modifier.weight(1f) else Modifier)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        content(close)
                    }
                    Column(modifier = Modifier.padding(bottom = bottomInset)) {
                        footer(close)
                    }
                }
            }
        }
    }
}

private data class SystemBarInsets(
    val status: Int,
    val navigation: Int,
)

/**
 * Dialog Compose berada di antara status bar dan navigation bar, sedangkan
 * root Compose tetap setinggi layar. Keduanya dipakai untuk menggeser sheet
 * agar bagian bawahnya berhenti tepat di atas navigation bar.
 */
private fun readSystemBarInsets(context: Context): SystemBarInsets {
    val resources = context.resources
    fun dimen(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else 0
    }

    val fallbackStatus = dimen("status_bar_height")
    val fallbackNavigation = dimen("navigation_bar_height")
    val insets = context.findActivity()
        ?.let { ViewCompat.getRootWindowInsets(it.window.decorView) }
    val status = insets
        ?.getInsets(WindowInsetsCompat.Type.statusBars())
        ?.top
        ?.takeIf { it > 0 }
        ?: fallbackStatus
    val navigation = insets
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom
        ?.takeIf { it > 0 }
        ?: fallbackNavigation
    return SystemBarInsets(status = status, navigation = navigation)
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
