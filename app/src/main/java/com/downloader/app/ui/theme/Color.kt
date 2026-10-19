package com.downloader.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design token identitas visual "Downloader".
 *
 * Nilai diambil dari halaman **Design System** file Figma
 * "Downloader - UI Prototype v1.1.2 Build 8" (layer `Token / …`), bukan asumsi.
 * Jangan menulis warna literal di layar — selalu ambil dari [DownloaderColors]
 * (atau [ArtworkTones]/[QuickActionTones] untuk palet) agar identitas visual
 * bisa diubah dari satu tempat.
 */
object DownloaderColors {
    // ---- Permukaan ----
    /** Latar paling belakang aplikasi (sekaligus fill kolom pencarian di desain). */
    val Background = Color(0xFF080A10)

    /** Permukaan standar / isi kartu & baris. */
    val Surface = Color(0xFF151821)

    /** Permukaan terangkat: menu konteks, dialog, bottom sheet. */
    val SurfaceElevated = Color(0xFF1D2230)

    /** Permukaan "glass" mengapung (bottom nav). Di desain opacity 94%. */
    val Glass = Color(0xFF161D2A)

    /** Mini player — glass satu tingkat lebih terang dari [Surface]. */
    val GlassRaised = Color(0xFF171E2A)

    /** Permukaan alternatif untuk chip / placeholder ikon. */
    val SurfaceVariant = SurfaceElevated

    // ---- Garis tepi ----
    /** Garis tepi standar semua kartu & panel. */
    val Border = Color(0xFF2B3242)

    /** Border biru redup (kartu versi di layar Pembaruan, tombol repository). */
    val BorderSoftBlue = Color(0xFF294D80)

    val Outline = Border

    // ---- Aksen ----
    val Primary = Color(0xFF3B82F6)
    val PrimaryDeep = Color(0xFF2563EB)

    /** Biru terang: label nav aktif, judul lagu terpilih, aksen artwork. */
    val PrimaryLight = Color(0xFF60A5FA)

    /** Cincin 1px di sekeliling FAB di desain. */
    val PrimaryHalo = Color(0xFF5791FF)

    /** Biru lembut untuk latar ikon/badge/chip aktif. */
    val PrimarySoft = Color(0xFF142A4B)

    /** Aksen cyan (angka "Aktif", progres, ikon). */
    val Cyan = Color(0xFF38BDF8)

    /**
     * Teks di atas permukaan biru lembut ([PrimarySoft]).
     * Desain memakai `#D8E8FF`/`#DBEAFE`/`#D6E7FF`/`#BBD7FF`/`#BDD9FF`/`#CAD7EE`
     * secara bergantian (variasi ±8/255) — disatukan ke satu token.
     */
    val TextOnSoft = Color(0xFFD8E8FF)

    // ---- Teks ----
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFA2ABBC)

    /** Teks & ikon paling redup (nav non-aktif, jam pada riwayat, jumlah lagu). */
    val TextMuted = Color(0xFF717B8E)
    val TextFaint = TextMuted

    // ---- Status ----
    val Success = Color(0xFF22C55E)

    /** Latar badge/tile sukses (`#14382A`/`#123426`/`#153A2B` di desain → satu token). */
    val SuccessSoft = Color(0xFF14382A)

    val Warning = Color(0xFFFBBF24)

    /** Latar tile warning (`#2D2617` di desain). */
    val WarningSoft = Color(0xFF2D2617)

    val Danger = Color(0xFFF87171)

    /** Latar tile hapus/peringatan (`#3B202A`/`#39202A` di desain → satu token). */
    val DangerSoft = Color(0xFF3B202A)
}

/**
 * Palet tile ikon untuk aksi cepat "Unduh berdasarkan jenis" di Beranda.
 * Urutan & warnanya persis dari desain (Video → Audio → Foto → Playlist).
 */
object QuickActionTones {
    val Video = DownloaderColors.PrimarySoft
    val Audio = Color(0xFF1C2435)
    val Photo = Color(0xFF25213C)
    val Playlist = DownloaderColors.WarningSoft
}

object StatusColors {
    val warning = DownloaderColors.Warning
    val success = DownloaderColors.Success
    val danger = DownloaderColors.Danger
}
