package com.downloader.app.core.engine

import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.ErrorMapper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object YtDlpEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun dumpInfo(
        url: String,
        flatPlaylist: Boolean,
        playlistLimit: Int = 10000,
    ): JsonObject = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url)
            .addOption("--dump-single-json")
            .addOption("--skip-download")
            .addOption("--ignore-config")
            .addOption("--no-warnings")
            .addOption("--no-check-certificates")
            .addOption("--socket-timeout", 20)
            .addOption("--retries", 3)
            .addOption("--extractor-retries", 3)

        if (flatPlaylist) {
            request.addOption("--flat-playlist")
            request.addOption("--playlist-end", playlistLimit)
        } else {
            request.addOption("--no-playlist")
        }

        val response = try {
            execute(request, "info_${System.nanoTime()}") { _, _, _ -> }
        } catch (e: YoutubeDLException) {
            throw AppException("EXTRACTOR_FAILED", ErrorMapper.friendly(e.message))
        }

        val text = extractJsonObjectText(response.out)
            ?: throw AppException(
                "EXTRACTOR_FAILED",
                ErrorMapper.friendly(response.err.ifBlank { DownloaderApp.str(R.string.err_extractor_failed) }),
            )

        try {
            json.parseToJsonElement(text).jsonObject
        } catch (_: Exception) {
            throw AppException("EXTRACTOR_FAILED", DownloaderApp.str(R.string.err_metadata_unreadable))
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun execute(
        request: YoutubeDLRequest,
        processId: String,
        onLine: (Float, Long, String) -> Unit,
    ): YoutubeDLResponse = withContext(Dispatchers.IO) {
        val ctxJob: Job? = coroutineContext[Job]
        val killer = ctxJob?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
            runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
        }
        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                onLine(progress, eta, line)
            }
        } catch (e: YoutubeDL.CanceledException) {
            throw CancellationException("proses unduhan dihentikan")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("proses unduhan diinterupsi")
        } finally {
            killer?.dispose()
        }
    }

    private fun extractJsonObjectText(output: String): String? {
        val trimmed = output.trim()
        val start = trimmed.indexOf('{')
        if (start < 0) return null
        val end = trimmed.lastIndexOf('}')
        if (end <= start) return null
        return trimmed.substring(start, end + 1)
    }
}
