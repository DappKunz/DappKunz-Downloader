package com.downloader.app.core.util

import java.io.File
import java.text.Normalizer
import java.util.Locale

object FileNames {

    val DIRECT_IMAGE_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".svg")
    val DIRECT_VIDEO_EXTENSIONS = setOf(".mp4", ".mkv", ".webm", ".mov", ".avi", ".flv", ".ts")
    val DIRECT_AUDIO_EXTENSIONS = setOf(".mp3", ".m4a", ".flac", ".wav", ".aac", ".ogg", ".opus", ".wma")

    private val illegalChars = Regex("[\\\\/*?:\"<>|\\u0000-\\u001F]")
    private val whitespace = Regex("\\s+")

    fun sanitizeFilename(filename: String?, maxLength: Int = 200): String {
        if (filename.isNullOrBlank()) return "download"
        var name = Normalizer.normalize(filename, Normalizer.Form.NFKD)
        name = illegalChars.replace(name, "")
        name = whitespace.replace(name, " ").trim().trim('.')
        if (name.isEmpty()) name = "download"
        if (name.length > maxLength) name = name.take(maxLength).trimEnd().trim('.')
        return if (name.isEmpty()) "download" else name
    }

    fun uniqueFile(directory: File, fileName: String): File {
        val safe = sanitizeFilename(fileName)
        var candidate = File(directory, safe)
        if (!candidate.exists()) return candidate
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }

    fun extensionOf(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val dot = path.lastIndexOf('.')
        if (dot <= 0 || dot == path.length - 1) return ""
        return path.substring(dot).lowercase(Locale.ROOT)
    }

    fun isDirectImage(url: String): Boolean = extensionOf(url) in DIRECT_IMAGE_EXTENSIONS

    fun mimeFor(format: String): String = when (format.lowercase(Locale.ROOT)) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "zip" -> "application/zip"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }

    fun mimeForFile(file: File): String = mimeFor(file.extension.ifBlank { "bin" })

    fun formatDuration(totalSeconds: Int?): String {
        if (totalSeconds == null || totalSeconds <= 0) return ""
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var idx = 0
        while (value >= 1024 && idx < units.lastIndex) {
            value /= 1024
            idx++
        }
        return if (idx == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", value, units[idx])
    }
}
