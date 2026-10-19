package com.downloader.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.music.MusicController
import com.downloader.app.core.music.MusicLibrary
import com.downloader.app.core.service.JobController
import com.downloader.app.data.HistoryEntry
import com.downloader.app.ui.components.DownloaderBottomBar
import com.downloader.app.ui.donation.DonationScreen
import com.downloader.app.ui.download.DownloadPrefill
import com.downloader.app.ui.download.NewDownloadScreen
import com.downloader.app.ui.history.HistoryScreen
import com.downloader.app.ui.home.HomeScreen
import com.downloader.app.ui.home.HomeViewModel
import com.downloader.app.ui.music.MiniPlayer
import com.downloader.app.ui.music.MusicScreen
import com.downloader.app.ui.music.NowPlayingScreen
import com.downloader.app.ui.online.OnlineMusicScreen
import com.downloader.app.ui.settings.AboutScreen
import com.downloader.app.ui.settings.DebugScreen
import com.downloader.app.ui.settings.LogScreen
import com.downloader.app.ui.settings.SettingsScreen
import com.downloader.app.ui.settings.UpdateScreen
import kotlinx.coroutines.launch

enum class MainTab { HOME, MUSIC, ONLINE_MUSIC, SETTINGS }

@Composable
fun AppRoot(sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showNew by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showDebug by rememberSaveable { mutableStateOf(false) }
    var showUpdates by rememberSaveable { mutableStateOf(false) }
    var showDonation by rememberSaveable { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var prefill by remember { mutableStateOf<DownloadPrefill?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel()

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            prefill = DownloadPrefill(url = sharedUrl)
            tab = MainTab.HOME
            showNew = true
            onSharedUrlConsumed()
        }
    }

    val miniTrack by MusicController.currentTrack.collectAsState()

    LaunchedEffect(Unit) {
        JobController.events.collect { event ->
            val message = when (event) {
                is JobController.JobEvent.Completed ->
                    DownloaderApp.str(R.string.snackbar_completed, event.title)

                is JobController.JobEvent.Failed ->
                    DownloaderApp.str(R.string.snackbar_failed, event.title, event.message)

                is JobController.JobEvent.Cancelled ->
                    DownloaderApp.str(R.string.snackbar_cancelled, event.title)
            }
            if (event is JobController.JobEvent.Completed) {
                val format = JobController.jobs.value.firstOrNull { it.jobId == event.jobId }?.format
                if (format == "mp3" || format == "m4a") {
                    MusicLibrary.refreshIfLoaded(DownloaderApp.ctx())
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (miniTrack != null) {
                        MiniPlayer(onOpen = { showNowPlaying = true })
                    }
                    DownloaderBottomBar(
                        current = tab,
                        onTabSelected = { tab = it },
                        onNewDownload = {
                            prefill = null
                            showNew = true
                        },
                    )
                }
            },
        ) { padding ->
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = homeViewModel,
                    onQuickType = { format ->
                        prefill = DownloadPrefill(url = "", format = format)
                        showNew = true
                    },
                    onSeeAllHistory = { showHistory = true },
                    onRetry = { entry ->
                        prefill = entry.toPrefill()
                        showHistory = false
                        showNew = true
                    },
                    modifier = Modifier.padding(padding),
                )

                MainTab.MUSIC -> MusicScreen(
                    onOpenPlayer = { showNowPlaying = true },
                    modifier = Modifier.padding(padding),
                )

                MainTab.ONLINE_MUSIC -> OnlineMusicScreen(
                    onDownload = { url ->
                        prefill = DownloadPrefill(url = url)
                        showNew = true
                    },
                    modifier = Modifier.padding(padding),
                )

                MainTab.SETTINGS -> SettingsScreen(
                    onAbout = { showAbout = true },
                    onUpdate = { showUpdates = true },
                    onHistory = { showHistory = true },
                    onLog = { showLog = true },
                    onDebug = { showDebug = true },
                    onDonate = { showDonation = true },
                    modifier = Modifier.padding(padding),
                )
            }
        }

        Overlay(visible = showNew, onDismiss = { showNew = false }) {
            NewDownloadScreen(
                prefill = prefill,
                onPrefillConsumed = { prefill = null },
                onClose = { showNew = false },
                onStarted = {
                    showNew = false
                    tab = MainTab.HOME
                },
                onDuplicate = {
                    scope.launch {
                        snackbarHostState.showSnackbar(DownloaderApp.str(R.string.snackbar_duplicate))
                    }
                },
            )
        }

        Overlay(visible = showHistory, onDismiss = { showHistory = false }) {
            HistoryScreen(
                onClose = { showHistory = false },
                onRetry = { entry ->
                    prefill = entry.toPrefill()
                    showHistory = false
                    showNew = true
                },
            )
        }

        Overlay(visible = showAbout, onDismiss = { showAbout = false }) {
            AboutScreen(onClose = { showAbout = false })
        }

        Overlay(visible = showUpdates, onDismiss = { showUpdates = false }) {
            UpdateScreen(onClose = { showUpdates = false })
        }

        Overlay(visible = showLog, onDismiss = { showLog = false }) {
            LogScreen(onClose = { showLog = false })
        }

        Overlay(visible = showDonation, onDismiss = { showDonation = false }) {
            DonationScreen(onClose = { showDonation = false })
        }

        Overlay(visible = showNowPlaying, onDismiss = { showNowPlaying = false }) {
            NowPlayingScreen(onClose = { showNowPlaying = false })
        }

        Overlay(visible = showDebug, onDismiss = { showDebug = false }) {
            DebugScreen(
                onClose = { showDebug = false },
                onOpenLog = {
                    showDebug = false
                    showLog = true
                },
            )
        }
    }
}

@Composable
private fun Overlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(animationSpec = tween(300)) { it / 4 } + fadeIn(tween(240)),
        exit = slideOutHorizontally(animationSpec = tween(220)) { it / 4 } + fadeOut(tween(180)),
    ) {
        BackHandler(onBack = onDismiss)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                content()
            }
        }
    }
}

private fun HistoryEntry.toPrefill(): DownloadPrefill = DownloadPrefill(
    url = url,
    format = format,
    bitrate = bitrate,
    resolution = resolution,
    hd = hd,
    zip = zipArchive,
)
