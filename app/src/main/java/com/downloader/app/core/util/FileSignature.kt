package com.downloader.app.core.util

import java.io.File
import java.util.Locale

/**
 * Pemeriksaan signature berkas yang bisa diuji di JVM (tanpa Android API).
 *
 * Ini penjaga pertama sebelum file dipublikasikan: membaca maksimal 64 byte
 * dan memastikan magic byte cocok dengan format yang diminta. Bukan pengganti
 * probe media — file terpotong tetap bisa lolos di sini, sehingga pipeline
 * tetap menjalankan [MediaProbe] setelah pemeriksaan ini.
 */
object FileSignature {

    private const val HEAD_BYTES = 64
    private const val MIN_HEAD_BYTES = 4

    fun hasValidMagic(file: File, format: String): Boolean {
        if (!file.exists() || file.length() < MIN_HEAD_BYTES) return false
        val head = readHead(file) ?: return false
        return matches(head, format)
    }

    fun hasValidImage(file: File): Boolean = imageExtension(file) != null

    /** Ekstensi yang sesuai isi berkas gambar, atau null bila bukan gambar dikenal. */
    fun imageExtension(file: File): String? {
        if (!file.exists() || file.length() < MIN_HEAD_BYTES) return null
        val head = readHead(file) ?: return null
        return when {
            isJpeg(head) -> "jpg"
            isPng(head) -> "png"
            isWebp(head) -> "webp"
            isGif(head) -> "gif"
            else -> null
        }
    }

    internal fun matches(head: ByteArray, format: String): Boolean {
        if (head.size < MIN_HEAD_BYTES) return false
        return when (format.lowercase(Locale.ROOT)) {
            "mp3" -> isMp3(head)
            "m4a", "mp4", "mov" -> containsAscii(head, 4, "ftyp")
            "jpg", "jpeg" -> isJpeg(head)
            "png" -> isPng(head)
            "webp" -> isWebp(head)
            "gif" -> isGif(head)
            else -> true
        }
    }

    private fun readHead(file: File): ByteArray? = try {
        val buffer = ByteArray(HEAD_BYTES)
        val read = file.inputStream().use { input ->
            var offset = 0
            while (offset < buffer.size) {
                val n = input.read(buffer, offset, buffer.size - offset)
                if (n < 0) break
                offset += n
            }
            offset
        }
        buffer.copyOf(read)
    } catch (_: Exception) {
        null
    }

    private fun isMp3(head: ByteArray): Boolean {
        val id3 = head.size >= 3 &&
            head[0] == 'I'.code.toByte() &&
            head[1] == 'D'.code.toByte() &&
            head[2] == '3'.code.toByte()
        val sync = (head[0].toInt() and 0xFF) == 0xFF &&
            head.size >= 2 &&
            (head[1].toInt() and 0xE0) == 0xE0
        return id3 || sync
    }

    private fun containsAscii(head: ByteArray, from: Int, token: String): Boolean {
        if (head.size < from + token.length) return false
        for (start in from..(head.size - token.length)) {
            var found = true
            for (index in token.indices) {
                if (head[start + index].toInt().toChar() != token[index]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }

    private fun isJpeg(head: ByteArray): Boolean =
        head.size >= 3 &&
            (head[0].toInt() and 0xFF) == 0xFF &&
            (head[1].toInt() and 0xFF) == 0xD8 &&
            (head[2].toInt() and 0xFF) == 0xFF

    private fun isPng(head: ByteArray): Boolean =
        head.size >= 8 &&
            (head[0].toInt() and 0xFF) == 0x89 &&
            head[1] == 'P'.code.toByte() &&
            head[2] == 'N'.code.toByte() &&
            head[3] == 'G'.code.toByte() &&
            (head[4].toInt() and 0xFF) == 0x0D &&
            (head[5].toInt() and 0xFF) == 0x0A &&
            (head[6].toInt() and 0xFF) == 0x1A &&
            (head[7].toInt() and 0xFF) == 0x0A

    private fun isWebp(head: ByteArray): Boolean =
        head.size >= 12 &&
            head[0] == 'R'.code.toByte() &&
            head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() &&
            head[3] == 'F'.code.toByte() &&
            head[8] == 'W'.code.toByte() &&
            head[9] == 'E'.code.toByte() &&
            head[10] == 'B'.code.toByte() &&
            head[11] == 'P'.code.toByte()

    private fun isGif(head: ByteArray): Boolean =
        head.size >= 6 &&
            head[0] == 'G'.code.toByte() &&
            head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() &&
            head[3] == '8'.code.toByte() &&
            (head[4] == '7'.code.toByte() || head[4] == '9'.code.toByte()) &&
            head[5] == 'a'.code.toByte()
}
