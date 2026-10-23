package com.downloader.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Token identitas visual Fluxen.
 *
 * Warna diambil dari halaman **Design System** file Figma "Downloader - UI
 * Prototype v1.1.2 Build 8" (layer `Token / …`) dan dilengkapi varian terang
 * agar seluruh aplikasi bisa mengikuti mode gelap/terang serta palet aksen.
 *
 * Semua properti adalah getter yang membaca [runtime] (snapshot state). Dengan
 * begitu layar yang memakai `DownloaderColors.*` otomatis recompose ketika
 * mode atau aksen berubah — tanpa harus mengganti seluruh pemakaian token.
 *
 * Jangan menulis warna literal di layar — selalu ambil dari [DownloaderColors]
 * (atau [ArtworkTones]/[QuickActionTones] untuk palet).
 */
object DownloaderColors {
    private data class Runtime(val dark: Boolean, val accent: Color)

    private var runtime by mutableStateOf(Runtime(dark = true, accent = Color(0xFF3B82F6)))

    /**
     * Dipanggil oleh `DownloaderTheme`. Perubahan nilai memicu recomposition
     * pembaca token; nilai yang sama tidak memicu apa pun.
     */
    internal fun configure(dark: Boolean, accent: Color) {
        val next = Runtime(dark, accent)
        if (runtime != next) runtime = next
    }

    val isDark: Boolean get() = runtime.dark

    private val accent: Color get() = runtime.accent

    private val brand: Color get() = accessibleAccentColor(accent, runtime.dark)

    private fun pick(dark: Color, light: Color): Color = if (runtime.dark) dark else light

    private fun blend(base: Color, target: Color, amount: Float): Color =
        lerp(base, target, amount.coerceIn(0f, 1f))

    // ---- Permukaan ----
    /** Latar paling belakang aplikasi — biru muda, bukan putih. */
    val Background get() = pick(Color(0xFF080A10), Color(0xFFDDE8FA))

    /** Permukaan standar / isi kartu & baris (biru redup, bukan putih). */
    val Surface get() = pick(Color(0xFF151821), Color(0xFFEAF2FF))

    /** Permukaan terangkat: menu konteks, dialog, bottom sheet. */
    val SurfaceElevated get() = pick(Color(0xFF1D2230), Color(0xFFF2F7FF))

    /** Permukaan "glass" mengapung (bottom nav). */
    val Glass get() = pick(Color(0xFF161D2A), Color(0xFFE3EDFF))

    /** Mini player — glass satu tingkat lebih terang dari [Surface]. */
    val GlassRaised get() = pick(Color(0xFF171E2A), Color(0xFFEDF4FF))

    /** Permukaan alternatif untuk chip / placeholder ikon. */
    val SurfaceVariant get() = pick(Color(0xFF1D2230), Color(0xFFCFDEF5))

    // ---- Garis tepi ----
    /** Garis tepi standar semua kartu & panel. */
    val Border get() = pick(Color(0xFF2B3242), Color(0xFFA8BEDF))

    /** Border biru redup (kartu versi di layar Pembaruan, tombol repository). */
    val BorderSoftBlue get() = if (runtime.dark) {
        blend(brand, Background, 0.52f)
    } else {
        blend(brand, Surface, 0.50f)
    }

    val Outline get() = Border

    // ---- Aksen ----
    val Primary get() = brand
    val PrimaryDeep get() = if (runtime.dark) blend(brand, Color.Black, 0.16f) else blend(brand, Color.Black, 0.24f)

    /** Aksen lebih terang: label nav aktif, judul lagu terpilih, aksen artwork. */
    val PrimaryLight get() = if (runtime.dark) blend(brand, Color.White, 0.26f) else blend(brand, Color.Black, 0.12f)

    /** Cincin 1px di sekeliling FAB di desain. */
    val PrimaryHalo get() = if (runtime.dark) blend(brand, Color.White, 0.12f) else brand

    /** Warna lembut untuk latar ikon/badge/chip aktif. */
    val PrimarySoft get() = if (runtime.dark) {
        blend(brand, Background, 0.80f)
    } else {
        blend(brand, Surface, 0.82f)
    }

    /** Aksen sekunder (angka "Aktif", progres, ikon). */
    val Cyan get() = if (runtime.dark) blend(brand, Color.White, 0.30f) else blend(brand, Color.Black, 0.08f)

    /** Aksen koleksi favorit (Figma: `#A78BFA`). */
    val Favorite get() = if (runtime.dark) Color(0xFFA78BFA) else Color(0xFF7C5CE0)

    /**
     * Teks di atas permukaan aksen lembut ([PrimarySoft]).
     * Desain memakai `#D8E8FF`/`#DBEAFE`/`#D6E7FF`/`#BBD7FF`/`#BDD9FF`/`#CAD7EE`
     * secara bergantian (variasi ±8/255) — disatukan ke satu token.
     */
    val TextOnSoft get() = if (runtime.dark) blend(brand, Color.White, 0.66f) else blend(brand, Color.Black, 0.52f)

    /** Warna teks/ikon yang aman di atas [Primary]. */
    val OnPrimary get() = if (brand.luminance() > 0.48f) Color(0xFF071018) else Color.White

    // ---- Teks ----
    val TextPrimary get() = pick(Color(0xFFF8FAFC), Color(0xFF081A33))
    val TextSecondary get() = pick(Color(0xFFA2ABBC), Color(0xFF3E5273))

    /** Teks & ikon paling redup (nav non-aktif, jam pada riwayat, jumlah lagu). */
    val TextMuted get() = pick(Color(0xFF717B8E), Color(0xFF667B99))
    val TextFaint get() = TextMuted

    // ---- Status ----
    val Success get() = pick(Color(0xFF22C55E), Color(0xFF16A34A))

    /** Latar badge/tile sukses (`#14382A`/`#123426`/`#153A2B` di desain → satu token). */
    val SuccessSoft get() = pick(Color(0xFF14382A), Color(0xFFDCFCE7))

    val Warning get() = pick(Color(0xFFFBBF24), Color(0xFFB45309))

    /** Latar tile warning (`#2D2617` di desain). */
    val WarningSoft get() = pick(Color(0xFF2D2617), Color(0xFFFEF3C7))

    val Danger get() = pick(Color(0xFFF87171), Color(0xFFDC2626))

    /** Teks/ikon di atas [Danger]. */
    val OnDanger get() = if (Danger.luminance() > 0.35f) Color(0xFF2A0A0A) else Color.White

    /** Latar tile hapus/peringatan (`#3B202A`/`#39202A` di desain → satu token). */
    val DangerSoft get() = pick(Color(0xFF3B202A), Color(0xFFFEE2E2))

    // ---- Overlay Tampilan (Figma node 52:295 / 52:252) ----
    /** Permukaan bottom sheet Tampilan: periwinkle pada versi terang, navy pada versi gelap. */
    val AppearanceSheet get() = pick(Color(0xFF1D2230), Color(0xFFB7C9FA))

    /** Track segmented control mode. */
    val AppearanceControl get() = pick(Color(0xFF080A10), Color(0xFFD9D9D9))

    /** Permukaan field dropdown palet/font. Versi terang memakai abu Figma, bukan putih. */
    val AppearanceField get() = pick(Color(0xFF080A10), Color(0xFFD9D9D9))

    /** Handle kecil di atas bottom sheet. */
    val AppearanceHandle get() = Color(0xFF5D6575)

    /** Teks di dalam bottom sheet Tampilan. */
    val AppearanceText get() = pick(Color(0xFFF8FAFC), Color(0xFF0F172A))
    val AppearanceTextSecondary get() = pick(Color(0xFFA2ABBC), Color(0xFF3A4354))

    /** Border kontrol Tampilan (sama pada kedua versi sesuai Figma). */
    val AppearanceBorder get() = Color(0xFF2B3242)

    /**
     * Scrim overlay Tampilan. Di mode gelap tetap pekat seperti Figma; di mode
     * terang dibuat lebih ringan dan kebiruan supaya latar biru aplikasi tidak
     * berubah menjadi abu-abu/hitam.
     */
    val AppearanceScrim get() = if (runtime.dark) {
        Color(0xFF080A10).copy(alpha = 0.72f)
    } else {
        Color(0xFF102341).copy(alpha = 0.32f)
    }
}

/**
 * Menjaga aksen tetap terbaca di mode terang. Swatch Figma tetap memakai HEX
 * asli (mis. hijau `#4BF63B`), tetapi warna yang dipakai untuk teks/ikon/field
 * digelapkan sampai kontrasnya memadai di atas permukaan terang.
 */
fun accessibleAccentColor(accent: Color, dark: Boolean): Color {
    if (dark) return accent
    var candidate = accent
    var guard = 0
    while (candidate.luminance() > 0.18f && guard < 24) {
        candidate = lerp(candidate, Color.Black, 0.07f)
        guard++
    }
    return candidate
}

/**
 * Palet tile ikon untuk aksi cepat "Unduh berdasarkan jenis" di Beranda.
 * Urutan & warnanya persis dari desain (Video → Audio → Foto → Playlist).
 */
object QuickActionTones {
    val Video get() = DownloaderColors.PrimarySoft
    val Audio get() = if (DownloaderColors.isDark) Color(0xFF1C2435) else Color(0xFFD5E5F8)
    val Photo get() = if (DownloaderColors.isDark) Color(0xFF25213C) else Color(0xFFDEDDF8)
    val Playlist get() = DownloaderColors.WarningSoft
}

object StatusColors {
    val warning get() = DownloaderColors.Warning
    val success get() = DownloaderColors.Success
    val danger get() = DownloaderColors.Danger
}
