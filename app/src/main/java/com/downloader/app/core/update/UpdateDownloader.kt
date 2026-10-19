package com.downloader.app.core.update

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateDownloader {

    data class Progress(val downloaded: Long, val total: Long) {
        val percent: Int
            get() = if (total > 0) ((downloaded * 100) / total).coerceIn(0, 100).toInt() else 0
    }

    internal val defaultClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updatesDir(context: Context): File = File(context.cacheDir, "updates")

    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        client: OkHttpClient = defaultClient,
        onProgress: (Progress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = updatesDir(context).apply { mkdirs() }
        val part = File(dir, "$fileName.part")
        val target = File(dir, fileName)
        part.delete()
        target.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UpdateChecker.USER_AGENT)
            .header("Accept", "application/octet-stream")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("empty body")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(part).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastEmit = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit >= 120) {
                                lastEmit = now
                                onProgress(Progress(downloaded, total))
                            }
                        }
                    }
                }
            }
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            onProgress(Progress(target.length(), target.length()))
            target
        } catch (t: Throwable) {
            part.delete()
            target.delete()
            throw t
        }
    }

    fun cleanup(context: Context, keep: File? = null) {
        val dir = updatesDir(context)
        dir.listFiles()?.forEach { file ->
            if (keep == null || file.absolutePath != keep.absolutePath) file.delete()
        }
    }
}
