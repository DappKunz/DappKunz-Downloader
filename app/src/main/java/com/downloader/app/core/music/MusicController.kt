package com.downloader.app.core.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MusicController {

    const val REPEAT_OFF = 0
    const val REPEAT_ALL = 1
    const val REPEAT_ONE = 2

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: ExoPlayer? = null
    private var pollJob: Job? = null

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(REPEAT_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @Synchronized
    fun ensure(context: Context): ExoPlayer {
        player?.let { return it }
        val appContext = context.applicationContext
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val created = ExoPlayer.Builder(appContext).build()
        created.setAudioAttributes(audioAttributes, true)
        created.setHandleAudioBecomingNoisy(true)
        created.setWakeMode(C.WAKE_MODE_LOCAL)
        created.addListener(controllerListener)
        player = created
        syncFromPlayer()
        return created
    }

    fun playerOrNull(): ExoPlayer? = player

    private val controllerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromPlayer()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFromPlayer()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            syncFromPlayer()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> REPEAT_ALL
                Player.REPEAT_MODE_ONE -> REPEAT_ONE
                else -> REPEAT_OFF
            }
            syncNavigation()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
            syncNavigation()
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message?.takeIf { it.isNotBlank() }
                ?: DownloaderApp.str(R.string.player_error_generic)
            syncFromPlayer()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            EqualizerController.attach(audioSessionId)
        }
    }

    fun playQueue(context: Context, tracks: List<Track>, index: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = index.coerceIn(0, tracks.size - 1)
        val target = tracks[safeIndex]
        if (!target.playable) {
            _error.value = DownloaderApp.str(R.string.player_error_unsupported)
            return
        }
        val playableTracks = tracks.filter { it.playable }
        val targetIndex = playableTracks.indexOfFirst { it.id == target.id }
        if (targetIndex < 0 || playableTracks.isEmpty()) return
        val appContext = context.applicationContext
        val exo = ensure(appContext)
        _error.value = null
        val sameQueue = _queue.value.map { it.id } == playableTracks.map { it.id }
        if (!sameQueue || exo.mediaItemCount != playableTracks.size) {
            _queue.value = playableTracks
            exo.setMediaItems(playableTracks.map { it.toMediaItem() })
            exo.prepare()
        }
        if (sameQueue && exo.currentMediaItem?.mediaId == target.id) {
            exo.play()
            startService(appContext)
            _isLoading.value = false
            startPolling()
            return
        }
        exo.seekToDefaultPosition(targetIndex)
        exo.play()
        startService(appContext)
        _isLoading.value = true
        startPolling()
    }

    /** Tambahkan lagu ke akhir antrean tanpa mengganggu lagu yang sedang diputar. */
    fun addToQueue(context: Context, track: Track) {
        insertIntoQueue(context, track, atEnd = true)
    }

    /** Sisipkan lagu tepat setelah lagu yang sedang diputar. */
    fun playNext(context: Context, track: Track) {
        insertIntoQueue(context, track, atEnd = false)
    }

    private fun insertIntoQueue(context: Context, track: Track, atEnd: Boolean) {
        if (!track.playable) {
            _error.value = DownloaderApp.str(R.string.player_error_unsupported)
            return
        }
        val exo = ensure(context.applicationContext)
        val currentIndex = exo.currentMediaItemIndex
        val insertAt = when {
            exo.mediaItemCount == 0 || currentIndex < 0 -> 0
            atEnd -> exo.mediaItemCount
            else -> (currentIndex + 1).coerceAtMost(exo.mediaItemCount)
        }
        exo.addMediaItem(insertAt, track.toMediaItem())
        val updated = _queue.value.toMutableList()
        updated.add(insertAt.coerceIn(0, updated.size), track)
        _queue.value = updated
        // Antrean yang tadinya kosong perlu disiapkan agar bisa langsung diputar.
        if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
        syncFromPlayer()
    }

    /** Lompat ke lagu ke-[index] pada antrean. */
    fun skipToQueueIndex(context: Context, index: Int) {
        val exo = player ?: return
        if (index !in 0 until exo.mediaItemCount) return
        _error.value = null
        exo.seekToDefaultPosition(index)
        exo.play()
        startService(context.applicationContext)
        _isLoading.value = true
        startPolling()
    }

    /** Buang satu lagu dari antrean berdasarkan posisinya. */
    fun removeFromQueue(index: Int) {
        val exo = player ?: return
        if (index !in 0 until exo.mediaItemCount) return
        exo.removeMediaItem(index)
        val updated = _queue.value.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        _queue.value = updated
        syncFromPlayer()
    }

    fun toggle(context: Context) {
        val exo = ensure(context.applicationContext)
        if (_currentTrack.value == null) return
        _error.value = null
        if (exo.isPlaying) {
            exo.pause()
            _positionMs.value = exo.currentPosition.coerceAtLeast(0)
        } else {
            exo.play()
            startService(context.applicationContext)
            startPolling()
        }
    }

    fun next() {
        val exo = player ?: return
        if (!exo.hasNextMediaItem()) return
        _error.value = null
        exo.seekToNextMediaItem()
        exo.play()
        startPolling()
    }

    fun previous() {
        val exo = player ?: return
        if (exo.currentPosition > 3000) {
            exo.seekTo(0)
            return
        }
        if (!exo.hasPreviousMediaItem()) {
            exo.seekTo(0)
            return
        }
        _error.value = null
        exo.seekToPreviousMediaItem()
        exo.play()
        startPolling()
    }

    fun seekTo(positionMs: Long) {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0 } ?: _durationMs.value
        exo.seekTo(positionMs.coerceIn(0, duration.coerceAtLeast(0)))
        _positionMs.value = exo.currentPosition
    }

    fun toggleShuffle() {
        val exo = player ?: return
        exo.shuffleModeEnabled = !exo.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val exo = player ?: return
        exo.repeatMode = when (exo.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun startService(context: Context) {
        // Jangan pakai startForegroundService: MediaSessionService (Media3) baru memanggil
        // startForeground saat notifikasi media aktif. Bila lagu lambat/gagal dimuat, sistem
        // melempar ForegroundServiceDidNotStartInTimeException -> force close.
        // startService biasa aman dari UI (app foreground); Media3 menaikkan service
        // ke foreground sendiri ketika playback benar-benar berjalan.
        runCatching {
            context.startService(Intent(context, PlaybackService::class.java))
        }
    }

    private fun syncFromPlayer() {
        val exo = player ?: return
        _isPlaying.value = exo.isPlaying
        _isLoading.value = exo.playbackState == Player.STATE_BUFFERING
        _shuffle.value = exo.shuffleModeEnabled
        _repeatMode.value = when (exo.repeatMode) {
            Player.REPEAT_MODE_ALL -> REPEAT_ALL
            Player.REPEAT_MODE_ONE -> REPEAT_ONE
            else -> REPEAT_OFF
        }
        val mediaId = exo.currentMediaItem?.mediaId
        val track = _queue.value.firstOrNull { it.id == mediaId }
        if (track != null && track != _currentTrack.value) {
            _currentTrack.value = track
        } else if (track == null && exo.mediaItemCount == 0) {
            _currentTrack.value = null
        }
        _durationMs.value = exo.duration.takeIf { it > 0 } ?: (track?.durationMs ?: 0L)
        _positionMs.value = exo.currentPosition.coerceAtLeast(0)
        syncNavigation()
        EqualizerController.attach(exo.audioSessionId)
        if (exo.isPlaying || exo.playbackState == Player.STATE_BUFFERING) startPolling()
    }

    private fun syncNavigation() {
        val exo = player ?: run {
            _hasNext.value = false
            _hasPrevious.value = false
            return
        }
        _hasNext.value = exo.hasNextMediaItem()
        _hasPrevious.value = exo.hasPreviousMediaItem() || exo.currentPosition > 0
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                val exo = player
                if (exo == null) {
                    _positionMs.value = 0L
                    break
                }
                _positionMs.value = exo.currentPosition.coerceAtLeast(0)
                val duration = exo.duration.takeIf { it > 0 }
                if (duration != null) _durationMs.value = duration
                _isPlaying.value = exo.isPlaying
                _isLoading.value = exo.playbackState == Player.STATE_BUFFERING
                syncNavigation()
                if (!exo.isPlaying && exo.playbackState != Player.STATE_BUFFERING) break
                delay(500)
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { runCatching { Uri.parse(it) }.getOrNull() })
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }
}
