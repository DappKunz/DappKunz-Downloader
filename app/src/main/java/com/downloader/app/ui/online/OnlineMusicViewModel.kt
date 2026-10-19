package com.downloader.app.ui.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.engine.YtDlpEngine
import com.downloader.app.core.util.floatOrNull
import com.downloader.app.core.util.stringOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Satu hasil pencarian online. */
data class OnlineTrack(
    val id: String,
    val url: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Int?,
    val thumbnail: String?,
)

sealed interface OnlineMusicState {
    object Idle : OnlineMusicState
    data class Searching(val query: String) : OnlineMusicState
    data class Results(val query: String, val tracks: List<OnlineTrack>) : OnlineMusicState
    data class Failed(val query: String, val message: String) : OnlineMusicState
}

/**
 * Pencarian "Online Music" memakai mesin yt-dlp yang sudah ada di aplikasi
 * (`ytsearch`) — bukan data contoh. Hasilnya adalah metadata nyata dari sumber
 * yang memang didukung; mengunduhnya dilakukan lewat alur unduhan biasa.
 */
class OnlineMusicViewModel : ViewModel() {

    private val _state = MutableStateFlow<OnlineMusicState>(OnlineMusicState.Idle)
    val state: StateFlow<OnlineMusicState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        searchJob?.cancel()
        _state.value = OnlineMusicState.Searching(cleaned)
        searchJob = viewModelScope.launch {
            try {
                val info = YtDlpEngine.dumpInfo(
                    url = "ytsearch$SEARCH_LIMIT:$cleaned",
                    flatPlaylist = true,
                )
                _state.value = OnlineMusicState.Results(cleaned, parseEntries(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = OnlineMusicState.Failed(
                    query = cleaned,
                    message = e.message?.takeIf { it.isNotBlank() }
                        ?: DownloaderApp.str(R.string.online_music_error_title),
                )
            }
        }
    }

    private fun parseEntries(info: JsonObject): List<OnlineTrack> {
        val entries = info["entries"] as? JsonArray ?: return emptyList()
        return entries.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj.stringOrNull("id") ?: return@mapNotNull null
            val title = obj.stringOrNull("title") ?: return@mapNotNull null
            val rawUrl = obj.stringOrNull("url")
            OnlineTrack(
                id = id,
                url = if (rawUrl != null && rawUrl.startsWith("http")) {
                    rawUrl
                } else {
                    "https://www.youtube.com/watch?v=$id"
                },
                title = title,
                uploader = obj.stringOrNull("uploader") ?: obj.stringOrNull("channel"),
                durationSeconds = obj.floatOrNull("duration")?.toInt()?.takeIf { it > 0 },
                thumbnail = obj.stringOrNull("thumbnail")
                    ?: "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            )
        }
    }

    private companion object {
        const val SEARCH_LIMIT = 15
    }
}
