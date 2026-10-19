package com.downloader.app.core.util

/**
 * Parser keluaran FFmpeg untuk progres konversi nyata:
 * banner `Duration: HH:MM:SS.xx` dan baris `-progress pipe:1`
 * (`out_time_us=` / `out_time_ms=`; keduanya dalam mikrodetik).
 */
object FfmpegProgress {

    private val durationRegex = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""")

    fun parseDurationUs(line: String): Long? {
        val match = durationRegex.find(line) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toDoubleOrNull() ?: return null
        if (seconds < 0) return null
        val micros = ((hours * 3600 + minutes * 60) * 1_000_000.0) + seconds * 1_000_000.0
        val rounded = micros.toLong()
        return rounded.takeIf { it > 0 }
    }

    fun parseOutTimeUs(line: String): Long? {
        val value = when {
            line.startsWith("out_time_us=") -> line.removePrefix("out_time_us=")
            line.startsWith("out_time_ms=") -> line.removePrefix("out_time_ms=")
            else -> return null
        }
        val micros = value.trim().toLongOrNull() ?: return null
        return micros.takeIf { it >= 0 }
    }
}
