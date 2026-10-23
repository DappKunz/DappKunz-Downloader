package com.downloader.app.core.storage

/**
 * Preferensi tampilan Fluxen: mode terang/gelap, palet aksen, dan keluarga font.
 *
 * Enum disimpan sebagai string stabil di [PrefsRepository] supaya nilai lama
 * tidak rusak ketika urutan enum berubah.
 */
enum class ThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class AccentPalette(val key: String, val hex: String?) {
    BLUE("blue", "#3B82F6"),
    RED("red", "#F63B3B"),
    GREEN("green", "#4BF63B"),
    CUSTOM("custom", null),
    ;

    companion object {
        fun fromKey(key: String?): AccentPalette = entries.firstOrNull { it.key == key } ?: BLUE
    }
}

enum class FontChoice(val key: String) {
    DEFAULT("default"),
    SYSTEM("system"),
    ARKUN_SANS("arkun_sans"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromKey(key: String?): FontChoice = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

private val HexColorPattern = Regex("^#[0-9A-Fa-f]{6}$")

/**
 * Menormalkan input HEX pengguna menjadi `#RRGGBB`.
 *
 * `#` di depan opsional agar pengguna yang mengetik `3B82F6` tidak langsung
 * dianggap salah. Mengembalikan `null` bila nilai tidak valid.
 */
fun normalizeHexColor(value: String): String? {
    val raw = value.trim()
    if (raw.isEmpty()) return null
    val withHash = if (raw.startsWith("#")) raw else "#$raw"
    if (!HexColorPattern.matches(withHash)) return null
    return withHash.uppercase()
}

fun isHexColor(value: String): Boolean = normalizeHexColor(value) != null
