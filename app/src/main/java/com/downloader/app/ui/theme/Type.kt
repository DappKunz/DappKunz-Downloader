package com.downloader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.downloader.app.R

/**
 * DM Sans — keluarga font utama aplikasi.
 *
 * Font menggunakan bobot Regular/Medium/SemiBold/Bold yang tersedia
 * di res/font. Ukuran teks, line-height, dan letter spacing mengikuti
 * spesifikasi desain Downloader.
 *
 * Lisensi font: SIL Open Font License 1.1.
 * Berkas lisensi tersedia di assets/licenses/DMSans-OFL.txt.
 */

val DownloaderFontFamily = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

private fun dm(
    size: Float,
    weight: FontWeight,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = DownloaderFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val DownloaderTypography = Typography(
    displayLarge = dm(30f, FontWeight.Bold, 39f),
    displayMedium = dm(25f, FontWeight.Bold, 32.6f),
    displaySmall = dm(22f, FontWeight.Bold, 28.6f),
    headlineLarge = dm(24f, FontWeight.Bold, 31.2f),
    headlineMedium = dm(22f, FontWeight.Bold, 28.6f),
    headlineSmall = dm(20f, FontWeight.Bold, 26f),
    titleLarge = dm(17f, FontWeight.SemiBold, 22.1f),
    titleMedium = dm(16f, FontWeight.Medium, 20.8f),
    titleSmall = dm(14f, FontWeight.Medium, 18.2f),
    bodyLarge = dm(14f, FontWeight.Normal, 18.2f),
    bodyMedium = dm(12f, FontWeight.Normal, 15.6f),
    bodySmall = dm(11f, FontWeight.Normal, 14.3f),
    labelLarge = dm(14f, FontWeight.Bold, 18.2f),
    labelMedium = dm(12f, FontWeight.Medium, 15.6f),
    labelSmall = dm(10f, FontWeight.Medium, 13f),
)

/**
 * Gaya teks bernama untuk komponen (nilai persis dari desain) agar ukuran
 * tidak ditulis berulang di layar. Pakai ini alih-alih `fontSize = …sp`.
 */
object DownloaderText {
    // Judul layar
    val hero = dm(25f, FontWeight.Bold, 32.6f)
    val screenTitle = dm(22f, FontWeight.Bold, 28.6f)
    val screenTitleLg = dm(23f, FontWeight.Bold, 29.9f)
    val screenTitleSm = dm(21f, FontWeight.Bold, 27.3f)
    val sheetTitle = dm(22f, FontWeight.Bold, 28.6f)
    val dialogTitle = dm(19f, FontWeight.Bold, 24.7f)
    val cardTitle = dm(13f, FontWeight.Bold, 16.9f)
    val statValue = dm(22f, FontWeight.Bold, 28.6f)
    val statValueSm = dm(17f, FontWeight.Bold, 22.1f)

    /** Nama developer di kartu "Tentang" (desain: 15·700). */
    val nameLg = dm(15f, FontWeight.Bold, 19.5f)

    // Section & isi
    val sectionTitle = dm(16f, FontWeight.Bold, 20.8f)
    val sectionTitleSm = dm(14f, FontWeight.Bold, 18.2f)
    val sectionLink = dm(11f, FontWeight.Medium, 14.3f)
    val rowTitle = dm(12f, FontWeight.Medium, 15.6f)
    val rowTitleSm = dm(11f, FontWeight.Medium, 14.3f)
    val body = dm(11f, FontWeight.Normal, 14.3f)
    val meta = dm(10f, FontWeight.Normal, 13f)
    val metaSm = dm(9f, FontWeight.Normal, 11.7f)
    val micro = dm(8f, FontWeight.Bold, 10.4f)
    val status = dm(9f, FontWeight.Medium, 11.7f)

    // Kontrol
    val button = dm(14f, FontWeight.Bold, 18.2f)
    val buttonSm = dm(12f, FontWeight.Bold, 15.6f)
    val chip = dm(10f, FontWeight.Medium, 13f)
    val navLabel = dm(9f, FontWeight.Medium, 11.7f)
}
