package com.downloader.app.core.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Progres tingkat job playlist: item yang sedang berjalan harus ikut
 * menyumbang, supaya bar tidak mandek selama item-item pertama belum rampung.
 */
class PlaylistProgressTest {

    @Test
    fun mulaiDariTitikAwalSaatBelumAdaItemBerjalan() {
        assertEquals(5, PlaylistProgress.percent(0, emptyList(), 10))
    }

    @Test
    fun itemYangBerjalanMenaikkanPersen() {
        // 1 dari 10 item baru separuh -> sumbangan 0,5 item.
        assertEquals(9, PlaylistProgress.percent(0, listOf(50), 10))
    }

    @Test
    fun empatItemParalelTerlihatBergerak() {
        // Persis keluhan: sebelumnya bar mandek di ~5% sampai item pertama
        // selesai. Sekarang empat unduhan yang berjalan separuh jalan sudah
        // terlihat sebagai 2 item setara.
        assertEquals(22, PlaylistProgress.percent(0, listOf(50, 50, 50, 50), 10))
    }

    @Test
    fun itemSelesaiTidakMembuatPersenMelonjak() {
        // Item yang rampung dilepas dari daftar berjalan tepat saat hitungan
        // selesai bertambah, jadi angkanya harus sama (tidak mundur, tidak naik
        // mendadak).
        val sebelum = PlaylistProgress.percent(0, listOf(100), 10)
        val sesudah = PlaylistProgress.percent(1, emptyList(), 10)
        assertEquals(sebelum, sesudah)
    }

    @Test
    fun persenTidakPernahMundurSelamaItemBerjalan() {
        var sebelumnya = PlaylistProgress.percent(0, emptyList(), 10)
        for (pct in 0..100 step 5) {
            val kini = PlaylistProgress.percent(0, listOf(pct), 10)
            assertTrue("persen mundur pada $pct: $sebelumnya -> $kini", kini >= sebelumnya)
            sebelumnya = kini
        }
    }

    @Test
    fun semuaItemSelesaiMencapaiBatasAkhir() {
        assertEquals(90, PlaylistProgress.percent(10, emptyList(), 10))
    }

    @Test
    fun sumbanganTidakMelebihiJumlahItem() {
        // Nilai aneh dari sink tidak boleh membuat bar melewati batas tahap ini.
        assertEquals(90, PlaylistProgress.percent(10, listOf(100, 100, 100), 10))
    }

    @Test
    fun nilaiItemDiluarRentangDijepit() {
        assertEquals(
            PlaylistProgress.percent(0, listOf(100, 0), 10),
            PlaylistProgress.percent(0, listOf(500, -20), 10),
        )
    }

    @Test
    fun playlistSatuItemTetapPunyaRuangSampaiSembilanPuluh() {
        assertEquals(47, PlaylistProgress.percent(0, listOf(50), 1))
        assertEquals(90, PlaylistProgress.percent(1, emptyList(), 1))
    }

    @Test
    fun totalNolTidakMembagiDenganNol() {
        assertEquals(5, PlaylistProgress.percent(0, emptyList(), 0))
    }
}
