package com.downloader.app.core.engine

import android.content.Context
import android.util.Log
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.log.AppLog
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Collections
import kotlin.coroutines.coroutineContext

object FfmpegEngine {

    private const val TAG = "FfmpegEngine"

    /**
     * Batas global proses FFmpeg yang berjalan bersamaan. Playlist besar bisa
     * memproses belasan item paralel; tanpa batas ini CPU/suhu/RAM jenuh dan
     * total waktu malah membengkak. Nilai konservatif karena encoding yt-dlp
     * (merge) tetap berjalan di luar gerbang ini.
     */
    private const val MAX_CONCURRENT = 2

    private val gate = Semaphore(MAX_CONCURRENT)

    private val running = Collections.synchronizedMap(HashMap<String, Process>())

    fun binary(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")

    private fun packagesDir(context: Context): File =
        File(context.noBackupFilesDir, "youtubedl-android/packages")

    /**
     * Folder paket hasil unzip libffmpeg.zip.so / libpython.zip.so yang dipakai
     * library youtubedl-android. FFmpeg memuat rantai dependensi
     * (libavfilter -> librubberband/libvmaf/libopenmpt -> libc++_shared.so)
     * sehingga semua folder ini wajib ada di LD_LIBRARY_PATH.
     */
    private fun libraryPath(context: Context): String {
        val packages = packagesDir(context)
        val dirs = listOf(
            File(packages, "python/usr/lib"),
            File(packages, "ffmpeg/usr/lib"),
            File(packages, "aria2c/usr/lib"),
            File(context.applicationInfo.nativeLibraryDir),
        )
        return dirs.filter { it.isDirectory }.joinToString(":") { it.absolutePath }
    }

    private suspend fun ensurePackages(context: Context) = withContext(Dispatchers.IO) {
        val ffmpegLibDir = File(packagesDir(context), "ffmpeg/usr/lib")
        if (ffmpegLibDir.isDirectory) return@withContext
        try {
            FFmpeg.getInstance().init(context)
        } catch (t: Throwable) {
            Log.e(TAG, "inisialisasi paket FFmpeg gagal", t)
        }
        if (!ffmpegLibDir.isDirectory) {
            throw AppException("FFMPEG_FAILED", DownloaderApp.str(R.string.err_ffmpeg_missing))
        }
    }

    sealed interface ProbeResult {
        /** ffprobe berjalan dan menghasilkan JSON. */
        data class Output(val json: String) : ProbeResult

        /** ffprobe menolak berkas (rusak/terpotong). */
        data object Failed : ProbeResult

        /** ffprobe tidak bisa dijalankan di perangkat ini. */
        data object Unavailable : ProbeResult
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun run(
        context: Context,
        args: List<String>,
        id: String,
        onLine: ((String) -> Unit)? = null,
    ): Int = withContext(Dispatchers.IO) {
        ensurePackages(context)

        val binary = binary(context)
        if (!binary.exists()) {
            throw AppException("FFMPEG_FAILED", DownloaderApp.str(R.string.err_ffmpeg_missing))
        }

        gate.withPermit {
            exec(context, binary, args, id, onLine)
        }
    }

    /**
     * Probe kontainer dengan ffprobe yang dibundel (libffprobe.so). Hasil dipakai
     * pipeline untuk memastikan berkas benar-benar berisi stream + durasi, bukan
     * hanya magic byte yang benar.
     */
    suspend fun probe(
        context: Context,
        file: File,
        id: String,
    ): ProbeResult = withContext(Dispatchers.IO) {
        val ffprobe = File(context.applicationInfo.nativeLibraryDir, "libffprobe.so")
        if (!file.exists() || file.length() <= 0 || !ffprobe.exists()) {
            return@withContext ProbeResult.Unavailable
        }
        try {
            ensurePackages(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return@withContext ProbeResult.Unavailable
        }

        val args = listOf(
            "-v", "error",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            file.absolutePath,
        )
        val output = StringBuilder()
        val code = try {
            gate.withPermit {
                exec(context, ffprobe, args, id) { line -> output.appendLine(line) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return@withContext ProbeResult.Unavailable
        }

        if (code != 0 || output.isBlank()) {
            AppLog.d(TAG, "ffprobe menolak ${file.name} (exit=$code)")
            return@withContext ProbeResult.Failed
        }
        ProbeResult.Output(output.toString())
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun exec(
        context: Context,
        binary: File,
        args: List<String>,
        id: String,
        onLine: ((String) -> Unit)? = null,
    ): Int {
        val command = ArrayList<String>(args.size + 1)
        command.add(binary.absolutePath)
        command.addAll(args)

        val environment = mapOf(
            "LD_LIBRARY_PATH" to libraryPath(context),
            "TMPDIR" to context.cacheDir.absolutePath,
            "HOME" to context.cacheDir.absolutePath,
        )

        val process = try {
            ProcessBuilder(command)
                .apply { environment().putAll(environment) }
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            throw AppException("FFMPEG_FAILED", DownloaderApp.str(R.string.err_ffmpeg_start, e.message ?: ""))
        }

        running[id] = process
        val ctxJob: Job? = coroutineContext[Job]
        val killer = ctxJob?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
            runCatching { process.destroy() }
        }

        val tail = ArrayDeque<String>()
        val code = try {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (tail.size >= 25) tail.removeFirst()
                    tail.addLast(line)
                    onLine?.invoke(line)
                }
            }
            process.waitFor()
        } catch (e: IOException) {
            throw AppException("FFMPEG_FAILED", DownloaderApp.str(R.string.err_ffmpeg_crashed, e.message ?: ""))
        } finally {
            killer?.dispose()
            running.remove(id)
        }

        if (code != 0) {
            Log.e(
                TAG,
                "${binary.name} exit=$code\nargs: ${args.joinToString(" ")}\n${tail.joinToString("\n")}",
            )
        }
        return code
    }

    fun cancel(id: String) {
        running[id]?.destroy()
    }
}
