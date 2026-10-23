package com.downloader.app.ui.donation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.web.SafeWebLinks
import com.downloader.app.ui.components.MotionDialog
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Layar donasi: memuat halaman Saweria di dalam aplikasi.
 *
 * Batasan yang disengaja:
 * - Hanya HTTPS dari domain Saweria (dan gateway pembayarannya) yang dimuat di sini.
 * - Tautan ke luar selalu lewat konfirmasi pengguna, tidak pernah dibuka diam-diam.
 * - Skema berbahaya (file, javascript, data, content) ditolak dan tidak pernah dibuka.
 * - Tidak ada akses berkas lokal, tidak ada jembatan JavaScript, tidak ada unduhan otomatis.
 */
@Composable
fun DonationScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val backgroundArgb = MaterialTheme.colorScheme.background.toArgb()

    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Int?>(null) }
    var externalUrl by remember { mutableStateOf<String?>(null) }
    var pendingExternal by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(externalUrl) {
        if (externalUrl != null) pendingExternal = externalUrl
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(backgroundArgb)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = true
                // Halaman pembayaran tidak boleh turun ke HTTP tanpa enkripsi.
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccess = false
                allowContentAccess = false
                hardenFileUrlAccess()
                setSupportZoom(false)
                displayZoomControls = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
            }

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url?.toString().orEmpty()

                    // Subframe (mis. iframe pembayaran): hanya domain tepercaya, tanpa dialog.
                    if (!request.isForMainFrame) return !SafeWebLinks.isTrusted(url)

                    return when (val decision = SafeWebLinks.decide(url)) {
                        is SafeWebLinks.Decision.InApp -> false
                        is SafeWebLinks.Decision.External -> {
                            externalUrl = decision.url
                            true
                        }

                        is SafeWebLinks.Decision.Blocked -> {
                            notice = decision.reason.noticeRes()
                            true
                        }
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loading = true
                    failed = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        loading = false
                        failed = true
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                        loading = false
                        failed = true
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler,
                    error: SslError?,
                ) {
                    // Sertifikat bermasalah: selalu batalkan, tidak pernah dilanjutkan.
                    handler.cancel()
                    loading = false
                    failed = true
                    notice = R.string.donation_ssl_blocked
                }
            }

            loadUrl(SafeWebLinks.DONATION_URL)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(5000)
            notice = null
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .entrance(0, offsetY = 10),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (webView.canGoBack()) webView.goBack() else onClose() },
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DownloaderApp.str(R.string.donation_title),
                    style = DownloaderText.screenTitleSm,
                    color = DownloaderColors.TextPrimary,
                )
                Text(
                    text = DownloaderApp.str(R.string.donation_subtitle),
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }

            if (failed) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = DownloaderColors.Primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = DownloaderApp.str(R.string.donation_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = DownloaderApp.str(R.string.donation_error_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                failed = false
                                loading = true
                                webView.reload()
                            },
                            modifier = Modifier.pressScale(scaleDown = 0.96f),
                        ) {
                            Text(DownloaderApp.str(R.string.donation_reload))
                        }
                    }
                }
            }

            notice?.let { res ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = DownloaderColors.SurfaceElevated,
                    tonalElevation = 4.dp,
                ) {
                    Text(
                        text = DownloaderApp.str(res),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }

    MotionDialog(
        visible = externalUrl != null,
        onDismiss = { externalUrl = null },
    ) {
        val pending = pendingExternal
        if (pending != null) {
            val webLink = pending.startsWith("http://", true) || pending.startsWith("https://", true)
            Text(
                text = DownloaderApp.str(R.string.donation_external_title),
                style = DownloaderText.dialogTitle,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = DownloaderApp.str(R.string.donation_external_desc),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { externalUrl = null }) {
                    Text(DownloaderApp.str(R.string.action_cancel))
                }
                Spacer(Modifier.size(4.dp))
                TextButton(
                    onClick = {
                        externalUrl = null
                        if (!openExternally(context, pending)) {
                            notice = R.string.donation_external_missing
                        }
                    },
                ) {
                    Text(
                        text = DownloaderApp.str(
                            if (webLink) {
                                R.string.donation_external_browser
                            } else {
                                R.string.donation_external_app
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun WebSettings.hardenFileUrlAccess() {
    // Pada perangkat lama (API < 30) dua setelan ini masih aktif secara bawaan.
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
}

private fun SafeWebLinks.Reason.noticeRes(): Int = when (this) {
    SafeWebLinks.Reason.MALFORMED -> R.string.donation_blocked_malformed
    SafeWebLinks.Reason.UNSAFE_SCHEME -> R.string.donation_blocked_scheme
    SafeWebLinks.Reason.NOT_HTTPS -> R.string.donation_blocked_https
    SafeWebLinks.Reason.LOOPBACK -> R.string.donation_blocked_local
}

/**
 * Buka tautan di luar aplikasi setelah pengguna menyetujuinya.
 *
 * Untuk tautan `intent://` komponen/selector dibuang lebih dulu supaya halaman web tidak
 * bisa menunjuk komponen aplikasi tertentu (intent redirection). Skema berbahaya ditolak.
 */
private fun openExternally(context: Context, url: String): Boolean {
    val intent = buildExternalIntent(url) ?: return false
    if (intent.resolveActivity(context.packageManager) == null) return false
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun buildExternalIntent(url: String): Intent? {
    val intent = if (url.startsWith("intent:", ignoreCase = true)) {
        runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull() ?: return null
    } else {
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    intent.component = null
    intent.selector = null
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    val scheme = intent.data?.scheme?.lowercase(Locale.ROOT)
    if (SafeWebLinks.isUnsafeScheme(scheme)) return null
    return intent
}
