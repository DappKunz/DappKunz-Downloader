package com.downloader.app.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Menyalin font pilihan pengguna ke penyimpanan internal aplikasi.
 *
 * Menyalin file lebih aman daripada menyimpan URI: izin URI sementara bisa
 * hilang setelah reboot/picker ditutup, sedangkan file internal selalu bisa
 * dibaca saat aplikasi dibuka kembali. Format divalidasi lewat signature
 * TrueType/OpenType sebelum diaktifkan.
 */
object CustomFontManager {

    const val MAX_FONT_BYTES = 20L * 1024 * 1024

    private const val DIR = "appearance_fonts"

    enum class Failure {
        UNREADABLE,
        EMPTY,
        TOO_LARGE,
        INVALID_FORMAT,
    }

    sealed interface ImportResult {
        data class Success(val file: File, val displayName: String) : ImportResult
        data class Error(val reason: Failure) : ImportResult
    }

    fun importFont(context: Context, uri: Uri): ImportResult {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(context, uri) ?: "custom_font.ttf"
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val temp = File(dir, "custom_font.import")
        temp.delete()

        var copied = 0L
        try {
            val input = resolver.openInputStream(uri) ?: return ImportResult.Error(Failure.UNREADABLE)
            input.use { source ->
                temp.outputStream().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = source.read(buffer)
                        if (read <= 0) break
                        copied += read
                        if (copied > MAX_FONT_BYTES) {
                            temp.delete()
                            return ImportResult.Error(Failure.TOO_LARGE)
                        }
                        target.write(buffer, 0, read)
                    }
                }
            }
        } catch (_: Exception) {
            temp.delete()
            return ImportResult.Error(Failure.UNREADABLE)
        }

        if (copied == 0L) {
            temp.delete()
            return ImportResult.Error(Failure.EMPTY)
        }
        if (!hasValidFontSignature(temp)) {
            temp.delete()
            return ImportResult.Error(Failure.INVALID_FORMAT)
        }

        val extension = if (isOpenType(temp)) "otf" else "ttf"
        val target = File(dir, "custom_font.$extension")

        // Hanya satu font kustom yang disimpan agar tidak menumpuk.
        dir.listFiles()?.forEach { file ->
            if (file != temp && file != target) file.delete()
        }
        if (target.exists() && !target.delete()) {
            temp.delete()
            return ImportResult.Error(Failure.UNREADABLE)
        }

        val moved = temp.renameTo(target)
        if (!moved) {
            val success = runCatching { temp.copyTo(target, overwrite = true) }.isSuccess
            temp.delete()
            if (!success) return ImportResult.Error(Failure.UNREADABLE)
        }
        if (!target.isFile || target.length() == 0L) {
            target.delete()
            return ImportResult.Error(Failure.UNREADABLE)
        }
        return ImportResult.Success(target, displayName)
    }

    /** TrueType (`00 01 00 00`, `true`, `typ1`) atau OpenType CFF (`OTTO`). */
    fun hasValidFontSignature(file: File): Boolean {
        if (!file.isFile || file.length() < 12L) return false
        val header = ByteArray(4)
        val read = runCatching {
            file.inputStream().use { it.read(header) }
        }.getOrDefault(0)
        if (read != 4) return false
        val tag = String(header, Charsets.ISO_8859_1)
        if (tag == "OTTO" || tag == "true" || tag == "typ1" || tag == "ttcf") return true
        return header[0] == 0.toByte() &&
            header[1] == 1.toByte() &&
            header[2] == 0.toByte() &&
            header[3] == 0.toByte()
    }

    private fun isOpenType(file: File): Boolean {
        val header = ByteArray(4)
        val read = runCatching {
            file.inputStream().use { it.read(header) }
        }.getOrDefault(0)
        return read == 4 && String(header, Charsets.ISO_8859_1) == "OTTO"
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }
    }.getOrNull()
}
