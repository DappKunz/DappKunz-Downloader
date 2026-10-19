package com.downloader.app.core.update

/**
 * Konvensi versi rilis DappKunzXD-Downloader:
 *
 * Tag rilis GitHub berbentuk `v<major>.<minor>.<patch>` (contoh `v1.1.0`) dan
 * `versionCode` Android = `major * 10000 + minor * 100 + patch`
 * (contoh: 1.1.0 -> 10100, 1.10.0 -> 11000). [com.downloader.app] build.gradle.kts
 * mengikuti skema yang sama agar tag dapat dibandingkan dengan metadata
 * aplikasi terpasang tanpa menebak-nebak.
 */
object Versioning {

    private val PATTERN = Regex("^[vV]?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?$")

    fun parse(value: String?): List<Int>? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        val match = PATTERN.matchEntire(text) ?: return null
        val parts = mutableListOf<Int>()
        for (index in 1..3) {
            val group = match.groupValues[index]
            parts += if (group.isEmpty()) 0 else group.toIntOrNull() ?: return null
        }
        return parts
    }

    fun compare(a: List<Int>, b: List<Int>): Int {
        for (index in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(index) { 0 } - b.getOrElse(index) { 0 }
            if (diff != 0) return if (diff > 0) 1 else -1
        }
        return 0
    }

    fun toVersionCode(version: List<Int>): Long? {
        val major = version.getOrElse(0) { 0 }
        val minor = version.getOrElse(1) { 0 }
        val patch = version.getOrElse(2) { 0 }
        if (major < 0 || minor !in 0..99 || patch !in 0..99) return null
        return major * 10000L + minor * 100L + patch
    }
}
