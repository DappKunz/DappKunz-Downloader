package com.downloader.app.core.pipeline

/**
 * Perhitungan persentase progres tingkat job untuk unduhan playlist.
 *
 * Sebelumnya persentase hanya dihitung dari jumlah item yang **sudah selesai**
 * (`5 + 85 * selesai / total`). Karena item playlist berjalan paralel, bar
 * tingkat job mandek di angka awal selama beberapa item pertama belum ada yang
 * rampung, lalu melonjak mendekati akhir — terlihat seperti bar yang tidak
 * nyambung dengan proses nyata.
 *
 * Di sini item yang sedang berjalan ikut menyumbang sebesar **pecahan
 * unduhannya sendiri** (0..1 per item). Hasilnya bar bergerak mengikuti
 * kemajuan nyata, dan tetap monoton:
 *
 * - item mulai -> sumbangan 0, persen tidak berubah;
 * - unduhan item naik -> persen ikut naik;
 * - item selesai -> sumbangannya dilepas tepat saat jumlah "selesai" bertambah,
 *   sehingga persen tidak melompat maupun mundur.
 *
 * Rumusnya murni (tanpa Android/FFmpeg) supaya bisa diuji di JVM.
 */
internal object PlaylistProgress {

    /**
     * @param finished jumlah item yang sudah tuntas (berhasil, gagal, dibatalkan)
     * @param inFlightPercent persen unduhan tiap item yang masih berjalan (0..100)
     * @param total jumlah seluruh item
     * @param from titik awal bar tingkat job (persen)
     * @param span lebar rentang untuk item; sisanya disisihkan untuk tahap penutup
     */
    fun percent(
        finished: Int,
        inFlightPercent: Collection<Int>,
        total: Int,
        from: Int = 5,
        span: Int = 85,
    ): Int {
        if (total <= 0) return from
        val fractional = inFlightPercent.sumOf { it.coerceIn(0, 100).toLong() } / 100.0
        val effective = (finished + fractional).coerceAtMost(total.toDouble())
        return (from + span * effective / total).toInt().coerceIn(from, from + span)
    }
}
