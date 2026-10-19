package com.downloader.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Token jarak & radius. Dipakai oleh komponen dan layar supaya rhythm
 * layout konsisten (bukan angka ajaib yang tersebar).
 *
 * Angka bersumber dari pengukuran frame desain (lebar acuan 393, kolom konten 353).
 */
object DownloaderSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Padding horizontal standar untuk konten layar (desain: 20). */
    val screen = 20.dp

    /** Jarak aman di atas bottom bar + mini player. */
    val bottomBarSpace = 16.dp

    /** Inset kiri/kanan panel navigasi mengapung (desain: 12). */
    val navInset = 12.dp
}

/**
 * Ukuran komponen yang berulang. Semua dari pengukuran desain.
 */
object DownloaderDimens {
    val navBarHeight = 68.dp
    val navItemWidth = 64.dp
    val navItemHeight = 54.dp

    /** FAB tengah: diameter 50, menonjol 18 di atas tepi atas panel nav. */
    val fabSize = 50.dp
    val fabLift = 18.dp

    val searchHeight = 42.dp
    val chipHeight = 31.dp
    val chipHeightSm = 30.dp
    val buttonHeight = 46.dp
    val buttonHeightSm = 44.dp
    val rowHeight = 52.dp
    val statCardHeight = 66.dp
    val miniPlayerHeight = 48.dp

    /** Tile ikon: aksi cepat 28, baris pengaturan 36, artwork 40. */
    val iconTile = 28.dp
    val iconTileMd = 36.dp
    val iconTileLg = 40.dp

    val artworkSm = 36.dp
    val artworkMd = 44.dp
    val artworkLg = 48.dp

    /** Artwork kartu "Media Preview" layar Download (desain: 88×88). */
    val artworkXl = 88.dp

    /** Logo aplikasi di layar "Tentang" (desain: bulat 88×88). */
    val aboutLogo = 88.dp

    /** Avatar developer di kartu "Tentang" (desain: bulat 48×48). */
    val avatarLg = 48.dp

    /** Ikon bahasa Kotlin/Python di kartu App Build Information (desain ≈30). */
    val langIcon = 30.dp

    /** Tombol lebar "GitHub Repository" di layar Tentang (desain: tinggi 48). */
    val buttonHeightLg = 48.dp
}

object DownloaderRadius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 24.dp
    val pill = 999.dp

    // Radius tambahan dari desain (dipakai kartu/baris/tile spesifik).
    val iconTile = 9.dp
    val tileSmall = 10.dp
    val tile = 11.dp
    val artwork = 12.dp
    val row = 13.dp
    val field = 14.dp
    val card = 15.dp
    val cardLg = 16.dp
    val menu = 17.dp
    val hero = 20.dp
    val dialog = 22.dp
    val artworkLg = 24.dp
    val sheet = 24.dp
    val nav = 27.dp
}

val DownloaderShapes = Shapes(
    extraSmall = RoundedCornerShape(DownloaderRadius.sm),
    small = RoundedCornerShape(DownloaderRadius.sm),
    medium = RoundedCornerShape(DownloaderRadius.md),
    large = RoundedCornerShape(DownloaderRadius.lg),
    extraLarge = RoundedCornerShape(DownloaderRadius.xl),
)
