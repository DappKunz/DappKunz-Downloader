package com.downloader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.downloader.app.R
import com.downloader.app.core.storage.CustomFontManager
import com.downloader.app.core.storage.FontChoice
import java.io.File

/**
 * Primeform Pro — keluarga font aplikasi (permintaan pemilik proyek, 19 Okt 2026,
 * berkas dari `Resources/font`).
 *
 * Hanya bobot yang benar-benar dipakai aplikasi (Normal/Medium/SemiBold/Bold —
 * hasil audit pemakaian `FontWeight`) yang dibundel, memakai berkas OTF statis
 * supaya bobot 500/600/700 benar juga di Android 7–8 (API 24/25) yang belum
 * mendukung variable font.
 *
 * Catatan varian "Demo" dari foundry — penting diketahui:
 *  - charset terbatas (±76 glyph). Karakter seperti `•`, `—`, `&`, `:`, `(`, `)`
 *    tidak ada di dalam font; Android otomatis merendernya dengan font sistem.
 *  - lisensi varian demo umumnya hanya untuk preview/personal. Siapkan lisensi
 *    penuh bila aplikasi dirilis/dipakai komersial.
 *
 * Berkas DM Sans lama (SIL OFL 1.1 — `assets/licenses/DMSans-OFL.txt`) masih
 * tersimpan di `res/font`, tetapi aturan produk terbaru menetapkan Primeform
 * sebagai default. Opsi "Default" di sheet Tampilan mengikuti
 * [DownloaderFontFamily] ini.
 *
 * Skala mengikuti specimen resmi halaman Design System:
 * Display 30·700 · Heading 22·700 · Title 16·500 · Body 14·400 · Caption 12·400,
 * dengan line-height ≈ 1.30× (dibaca dari layer teks desain).
 */
val DownloaderFontFamily = FontFamily(
    Font(R.font.primeform_regular, FontWeight.Normal),
    Font(R.font.primeform_medium, FontWeight.Medium),
    Font(R.font.primeform_semibold, FontWeight.SemiBold),
    Font(R.font.primeform_bold, FontWeight.Bold),
)

/** Font sistem Android untuk opsi "System Font". */
val SystemFontFamily = FontFamily.SansSerif

/**
 * ArkunSans belum tersedia di repository/aset mana pun. Jangan menggantinya
 * diam-diam dengan font lain: UI menampilkan status "belum tersedia".
 */
const val ArkunSansAvailable: Boolean = false

/**
 * Keluarga font yang sedang aktif. Nilainya diatur oleh `DownloaderTheme`
 * setelah preferensi dibaca; getter-nya snapshot state agar seluruh teks yang
 * memakai [DownloaderText] ikut berubah tanpa restart.
 */
object DownloaderFonts {
    private var state by mutableStateOf<FontFamily>(DownloaderFontFamily)

    val family: FontFamily get() = state

    internal fun configure(family: FontFamily) {
        if (state != family) state = family
    }
}

private fun dm(
    size: Float,
    weight: FontWeight,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = DownloaderFonts.family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/** Membangun skala tipografi Material untuk keluarga font yang dipilih. */
fun buildDownloaderTypography(family: FontFamily): Typography {
    fun style(
        size: Float,
        weight: FontWeight,
        lineHeight: Float,
        letterSpacing: Float = 0f,
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )

    return Typography(
        displayLarge = style(30f, FontWeight.Bold, 39f),
        displayMedium = style(25f, FontWeight.Bold, 32.6f),
        displaySmall = style(22f, FontWeight.Bold, 28.6f),
        headlineLarge = style(24f, FontWeight.Bold, 31.2f),
        headlineMedium = style(22f, FontWeight.Bold, 28.6f),
        headlineSmall = style(20f, FontWeight.Bold, 26f),
        titleLarge = style(17f, FontWeight.SemiBold, 22.1f),
        titleMedium = style(16f, FontWeight.Medium, 20.8f),
        titleSmall = style(14f, FontWeight.Medium, 18.2f),
        bodyLarge = style(14f, FontWeight.Normal, 18.2f),
        bodyMedium = style(12f, FontWeight.Normal, 15.6f),
        bodySmall = style(11f, FontWeight.Normal, 14.3f),
        labelLarge = style(14f, FontWeight.Bold, 18.2f),
        labelMedium = style(12f, FontWeight.Medium, 15.6f),
        labelSmall = style(10f, FontWeight.Medium, 13f),
    )
}

/**
 * Menentukan keluarga font dari preferensi. Bila font kustom hilang/rusak,
 * fallback ke font default aplikasi (bukan crash dan bukan font acak).
 */
fun resolveAppFontFamily(choice: FontChoice, customFontPath: String?): FontFamily = when (choice) {
    FontChoice.DEFAULT -> DownloaderFontFamily
    FontChoice.SYSTEM -> SystemFontFamily
    FontChoice.ARKUN_SANS -> DownloaderFontFamily
    FontChoice.CUSTOM -> customFontFamily(customFontPath) ?: DownloaderFontFamily
}

fun customFontFamily(path: String?): FontFamily? {
    val file = path?.let(::File) ?: return null
    if (!file.isFile || !CustomFontManager.hasValidFontSignature(file)) return null
    return runCatching {
        FontFamily(
            Font(file, FontWeight.Normal),
            Font(file, FontWeight.Medium),
            Font(file, FontWeight.SemiBold),
            Font(file, FontWeight.Bold),
        )
    }.getOrNull()
}

/**
 * Gaya teks bernama untuk komponen (nilai persis dari desain) agar ukuran
 * tidak ditulis berulang di layar. Pakai ini alih-alih `fontSize = …sp`.
 */
object DownloaderText {
    // Judul layar
    val hero get() = dm(25f, FontWeight.Bold, 32.6f)
    val screenTitle get() = dm(22f, FontWeight.Bold, 28.6f)
    val screenTitleLg get() = dm(23f, FontWeight.Bold, 29.9f)
    val screenTitleSm get() = dm(21f, FontWeight.Bold, 27.3f)
    val sheetTitle get() = dm(22f, FontWeight.Bold, 28.6f)
    val dialogTitle get() = dm(19f, FontWeight.Bold, 24.7f)
    val cardTitle get() = dm(13f, FontWeight.Bold, 16.9f)
    val statValue get() = dm(22f, FontWeight.Bold, 28.6f)
    val statValueSm get() = dm(17f, FontWeight.Bold, 22.1f)

    /** Nama developer di kartu "Tentang" (desain: 15·700). */
    val nameLg get() = dm(15f, FontWeight.Bold, 19.5f)

    // Section & isi
    val sectionTitle get() = dm(16f, FontWeight.Bold, 20.8f)
    val sectionTitleSm get() = dm(14f, FontWeight.Bold, 18.2f)
    val groupTitle get() = dm(12f, FontWeight.Bold, 15.6f)
    val sectionLink get() = dm(11f, FontWeight.Medium, 14.3f)
    val rowTitle get() = dm(12f, FontWeight.Medium, 15.6f)
    val rowTitleSm get() = dm(11f, FontWeight.Medium, 14.3f)
    val body get() = dm(11f, FontWeight.Normal, 14.3f)
    val meta get() = dm(10f, FontWeight.Normal, 13f)
    val metaSm get() = dm(9f, FontWeight.Normal, 11.7f)
    val micro get() = dm(8f, FontWeight.Bold, 10.4f)
    val status get() = dm(9f, FontWeight.Medium, 11.7f)

    // Kontrol
    val button get() = dm(14f, FontWeight.Bold, 18.2f)
    val buttonSm get() = dm(12f, FontWeight.Bold, 15.6f)
    val chip get() = dm(10f, FontWeight.Medium, 13f)
    val navLabel get() = dm(9f, FontWeight.Medium, 11.7f)
}
