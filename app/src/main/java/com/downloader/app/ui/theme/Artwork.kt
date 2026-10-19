package com.downloader.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet artwork hasil desain: setiap tile digambar sebagai kotak berwarna dasar
 * dengan satu lingkaran aksen (opacity 0.65) di kanan-atas dan satu lingkaran
 * gelap di kiri-bawah — pola yang sama muncul di hampir semua layar
 * (cover lagu, album, playlist, thumbnail unduhan, artwork now playing).
 */
data class ArtworkTone(
    val base: Color,
    val accent: Color,
)

/**
 * Nilai base + aksen dibaca langsung dari node desain (mis. Album Card "Neon Lights"
 * cover `#1B3657` + aksen `#38BDF8`). Urutan deterministik: seed yang sama selalu
 * mendapat tone yang sama, sehingga tampilan tidak berubah-ubah antar render.
 */
object ArtworkTones {
    val Blue = ArtworkTone(Color(0xFF1B3657), Color(0xFF38BDF8))
    val Violet = ArtworkTone(Color(0xFF36304F), Color(0xFFA78BFA))
    val Rose = ArtworkTone(Color(0xFF4A2D39), Color(0xFFFB7185))
    val Teal = ArtworkTone(Color(0xFF174A4B), Color(0xFF5EEAD4))
    val Steel = ArtworkTone(Color(0xFF243047), Color(0xFF60A5FA))
    val Orchid = ArtworkTone(Color(0xFF423052), Color(0xFFA78BFA))
    val Amber = ArtworkTone(Color(0xFF5B3829), Color(0xFFFDBA74))
    val Sky = ArtworkTone(Color(0xFF243A52), Color(0xFF7DD3FC))
    val DeepBlue = ArtworkTone(Color(0xFF183653), Color(0xFF38BDF8))
    val Night = ArtworkTone(Color(0xFF172D43), Color(0xFF38BDF8))
    val Ink = ArtworkTone(Color(0xFF222B4C), Color(0xFFA78BFA))
    val Metal = ArtworkTone(Color(0xFF273056), Color(0xFF3B82F6))

    val All = listOf(Blue, Violet, Rose, Teal, Steel, Orchid, Amber, Sky, DeepBlue, Night, Ink, Metal)

    /** Pilih tone stabil dari nama berkas/lagu (bukan acak). */
    fun forSeed(seed: String?): ArtworkTone {
        val key = seed?.trim()?.lowercase().orEmpty()
        if (key.isEmpty()) return Blue
        val index = ((key.hashCode().toLong() and 0x7FFFFFFFL) % All.size).toInt()
        return All[index]
    }
}
