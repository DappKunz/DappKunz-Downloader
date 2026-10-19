package com.downloader.app.core.util

import android.os.StatFs
import java.io.File

/**
 * Preflight ruang penyimpanan sebelum unduhan dimulai.
 *
 * Sumber dan hasil proses berada di volume yang sama (filesDir), jadi ruang
 * yang dibutuhkan adalah kira-kira dua kali estimasi berkas ditambah margin
 * untuk berkas sementara, cover, dan potongan FFmpeg. Bila ukuran belum bisa
 * diperkirakan, tetap ada batas minimum agar disk yang hampir penuh tidak
 * menghasilkan kegagalan di tengah proses.
 */
object StorageGuard {

    private const val MIN_FREE_BYTES = 96L * 1024 * 1024
    private const val MARGIN_BYTES = 64L * 1024 * 1024
    private const val HEADROOM_FACTOR = 2L

    data class Space(val freeBytes: Long, val requiredBytes: Long) {
        val isEnough: Boolean get() = freeBytes >= requiredBytes
    }

    fun requiredBytes(estimateBytes: Long?): Long {
        val estimated = (estimateBytes ?: 0L).coerceAtLeast(0L)
        val scaled = if (estimated > (Long.MAX_VALUE - MARGIN_BYTES) / HEADROOM_FACTOR) {
            Long.MAX_VALUE
        } else {
            estimated * HEADROOM_FACTOR + MARGIN_BYTES
        }
        return maxOf(scaled, MIN_FREE_BYTES)
    }

    /** Null bila ukuran volume tidak bisa dibaca; pemanggil memilih melewati cek. */
    fun check(dir: File, estimateBytes: Long?): Space? {
        val free = try {
            StatFs(dir.absolutePath).availableBytes
        } catch (_: Exception) {
            return null
        }
        return Space(freeBytes = free, requiredBytes = requiredBytes(estimateBytes))
    }
}
