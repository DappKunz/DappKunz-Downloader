package com.downloader.app.core.engine

import android.content.Context
import com.downloader.app.core.AppException
import com.downloader.app.core.log.AppLog
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object EngineManager {

    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var started = false

    fun startAsync(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        _state.value = State.Preparing
        scope.launch {
            try {
                AppLog.d("Engine", "Mulai init YoutubeDL")
                YoutubeDL.getInstance().init(appContext)
                AppLog.d("Engine", "Mulai init FFmpeg")
                FFmpeg.getInstance().init(appContext)
                _state.value = State.Ready
                AppLog.d("Engine", "Mesin yt-dlp + FFmpeg siap")
                updateYtDlpQuietly(appContext)
            } catch (t: Throwable) {
                _state.value = State.Failed(
                    t.message ?: "Gagal menyiapkan mesin unduhan di perangkat."
                )
                AppLog.e("Engine", "Mesin gagal: ${t.message}")
                AppLog.e("Engine", t.stackTraceToString())
            }
        }
    }

    private fun updateYtDlpQuietly(context: Context) {
        try {
            val status = YoutubeDL.getInstance()
                .updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
            if (status == YoutubeDL.UpdateStatus.DONE) {
                YoutubeDL.getInstance().init(context)
            }
        } catch (_: Throwable) {
            // Offline / GitHub tidak terjangkau: lanjut memakai yt-dlp bawaan.
        }
    }

    suspend fun awaitReady() {
        val current = state.first { it is State.Ready || it is State.Failed }
        if (current is State.Failed) {
            throw AppException("ENGINE_FAILED", current.message)
        }
    }

    val isReady: Boolean
        get() = state.value is State.Ready
}
