package com.downloader.app.core.engine

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class Cover(
    val jpg: File? = null,
    val raw: File? = null,
)

object ThumbnailFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.bytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchBytesProgress(
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val total = body.contentLength()
                val sink = ByteArrayOutputStream(body.contentLength().coerceIn(0, 1_000_000).toInt().coerceAtLeast(8192))
                val buffer = ByteArray(16 * 1024)
                body.byteStream().use { input ->
                    var done = 0L
                    while (true) {
                        if (!isActive) throw CancellationException("unduhan foto dihentikan")
                        val read = input.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
                sink.toByteArray()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadCover(
        context: Context,
        candidates: List<String>,
        sourceDir: File,
        namePrefix: String = "cover",
    ): Cover = withContext(Dispatchers.IO) {
        if (candidates.isEmpty()) return@withContext Cover()

        // Nama unik per item: beberapa item playlist diproses paralel dalam satu
        // sourceDir yang sama, file cover bersama akan saling menimpa.
        val rawThumb = File(sourceDir, "${namePrefix}_raw_thumb")
        val jpgThumb = File(sourceDir, "${namePrefix}_cover.jpg")
        val pngThumb = File(sourceDir, "${namePrefix}_cover.png")

        for (url in candidates) {
            val bytes = fetchBytes(url) ?: continue
            if (bytes.size < 512) continue

            try {
                rawThumb.writeBytes(bytes)
            } catch (_: Exception) {
                continue
            }

            when (imageKind(bytes)) {
                ImageKind.JPEG -> {
                    try {
                        jpgThumb.writeBytes(bytes)
                        return@withContext Cover(jpg = jpgThumb)
                    } catch (_: Exception) {
                        continue
                    }
                }

                ImageKind.PNG -> {
                    var savedPng: File? = null
                    try {
                        pngThumb.writeBytes(bytes)
                        savedPng = pngThumb
                    } catch (_: Exception) {
                        savedPng = null
                    }
                    convertToJpeg(context, rawThumb, jpgThumb)
                    if (jpgThumb.exists() && jpgThumb.length() > 512) {
                        return@withContext Cover(jpg = jpgThumb, raw = savedPng)
                    }
                }

                else -> {
                    convertToJpeg(context, rawThumb, jpgThumb)
                    if (jpgThumb.exists() && jpgThumb.length() > 512) {
                        return@withContext Cover(jpg = jpgThumb)
                    }
                }
            }
        }
        Cover()
    }

    private suspend fun convertToJpeg(context: Context, input: File, output: File) {
        if (output.exists()) output.delete()
        try {
            FfmpegEngine.run(
                context,
                listOf("-y", "-threads", "0", "-i", input.absolutePath, "-vframes", "1", "-q:v", "3", output.absolutePath),
                "thumb_${System.nanoTime()}",
            )
        } catch (_: Exception) {
            // konversi gagal: pemanggil akan memakai kandidat/cadangan lain
        }
    }

    private fun imageKind(bytes: ByteArray): ImageKind {
        if (bytes.size < 12) return ImageKind.UNKNOWN
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return ImageKind.JPEG
        }
        if (bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) {
            return ImageKind.PNG
        }
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return ImageKind.WEBP
        }
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
            return ImageKind.GIF
        }
        return ImageKind.UNKNOWN
    }

    private enum class ImageKind { JPEG, PNG, WEBP, GIF, UNKNOWN }
}
