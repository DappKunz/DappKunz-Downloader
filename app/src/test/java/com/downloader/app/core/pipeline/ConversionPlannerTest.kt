package com.downloader.app.core.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test untuk keputusan operasi media.
 *
 * Fokusnya bukan "apakah hasilnya enak ditonton" (itu butuh perangkat), tetapi
 * dua hal yang bisa dibuktikan di JVM dan sebelumnya salah:
 *
 * - berkas yang **sudah sesuai format** tidak boleh menyentuh FFmpeg;
 * - berkas yang **butuh konversi** tidak boleh dicoba `-c copy` lebih dulu
 *   (Opus/Vorbis di dalam MP4 tidak berguna di Android).
 */
class ConversionPlannerTest {

    // --- Audio: sudah sesuai format (tanpa FFmpeg) ---

    @Test
    fun audioM4aDariKontainerM4aAdalahNative() {
        assertTrue(ConversionPlanner.isAudioNative("m4a", "m4a"))
        assertTrue(ConversionPlanner.isAudioNative("m4a", "mp4"))
        assertEquals(
            ConversionPlanner.AudioOperation.NATIVE,
            ConversionPlanner.planAudio("m4a", "m4a"),
        )
    }

    @Test
    fun audioMp3DariMp3AdalahNative() {
        assertTrue(ConversionPlanner.isAudioNative("mp3", "mp3"))
        assertEquals(
            ConversionPlanner.AudioOperation.NATIVE,
            ConversionPlanner.planAudio("mp3", "mp3"),
        )
    }

    @Test
    fun kontainerDiNormalisasi() {
        assertTrue(ConversionPlanner.isAudioNative("m4a", ".M4A"))
        assertTrue(ConversionPlanner.isAudioNative("mp3", "MP3"))
        assertTrue(ConversionPlanner.isAudioNative("m4a", " mp4 "))
    }

    // --- Audio: perlu konversi ---

    @Test
    fun audioAacAdtsTidakDianggapM4a() {
        // Jebakan "ganti ekstensi": .aac (ADTS) bukan kontainer MP4, jadi tidak
        // boleh hanya diganti nama menjadi .m4a.
        assertFalse(ConversionPlanner.isAudioNative("m4a", "aac"))
        assertEquals(
            ConversionPlanner.AudioOperation.TRANSCODE,
            ConversionPlanner.planAudio("m4a", "aac"),
        )
    }

    @Test
    fun audioOpusDanM4aKeMp3PerluKonversi() {
        assertFalse(ConversionPlanner.isAudioNative("mp3", "webm"))
        assertFalse(ConversionPlanner.isAudioNative("mp3", "m4a"))
        assertEquals(
            ConversionPlanner.AudioOperation.TRANSCODE,
            ConversionPlanner.planAudio("mp3", "m4a"),
        )
    }

    @Test
    fun audioMp3KeM4aPerluKonversi() {
        assertEquals(
            ConversionPlanner.AudioOperation.TRANSCODE,
            ConversionPlanner.planAudio("m4a", "mp3"),
        )
    }

    // --- Video: bisa langsung disimpan atau remux tanpa re-encode ---

    @Test
    fun videoH264AacCukupRemux() {
        assertEquals(
            ConversionPlanner.Operation.REMUX,
            ConversionPlanner.planVideo(videoCodec = "h264", audioCodec = "aac"),
        )
    }

    @Test
    fun videoHevcAacCukupRemux() {
        assertEquals(
            ConversionPlanner.Operation.REMUX,
            ConversionPlanner.planVideo(videoCodec = "hevc", audioCodec = "aac"),
        )
    }

    @Test
    fun videoTanpaAudioCukupRemux() {
        assertEquals(
            ConversionPlanner.Operation.REMUX,
            ConversionPlanner.planVideo(videoCodec = "h264", audioCodec = null),
        )
    }

    @Test
    fun codecBelumDiketahuiTidakMemaksaEncode() {
        // ffprobe tidak tersedia: pilih jalan termurah, validasi yang menolak
        // bila ternyata salah.
        assertEquals(
            ConversionPlanner.Operation.REMUX,
            ConversionPlanner.planVideo(videoCodec = null, audioCodec = null),
        )
    }

    // --- Video: audio tidak kompatibel -> satu kali jalan, tanpa re-encode video ---

    @Test
    fun videoVp9OpusTidakDicobaCopyDulu() {
        // Ini inti optimasinya: sebelumnya `-c copy` dijalankan lebih dulu dan
        // "berhasil" menghasilkan Opus di dalam MP4.
        assertEquals(
            ConversionPlanner.Operation.REMUX_AUDIO,
            ConversionPlanner.planVideo(videoCodec = "vp9", audioCodec = "opus"),
        )
    }

    @Test
    fun videoH264VorbisPerluEncodeAudioSaja() {
        assertEquals(
            ConversionPlanner.Operation.REMUX_AUDIO,
            ConversionPlanner.planVideo(videoCodec = "h264", audioCodec = "vorbis"),
        )
    }

    // --- Video: video tidak kompatibel -> langsung encode ---

    @Test
    fun videoVp8LangsungTranscodeTanpaDuaPercobaanSiaSia() {
        val plan = ConversionPlanner.planVideo(videoCodec = "vp8", audioCodec = "aac")
        assertEquals(ConversionPlanner.Operation.TRANSCODE, plan)
        assertTrue(ConversionPlanner.escalation(plan).isEmpty())
    }

    // --- Tangga cadangan (pemulihan saat FFmpeg/validasi gagal) ---

    @Test
    fun remuxNaikKeRemuxAudioLaluEncode() {
        assertEquals(
            listOf(ConversionPlanner.Operation.REMUX_AUDIO, ConversionPlanner.Operation.TRANSCODE),
            ConversionPlanner.escalation(ConversionPlanner.Operation.REMUX),
        )
    }

    @Test
    fun remuxAudioHanyaNaikKeEncode() {
        assertEquals(
            listOf(ConversionPlanner.Operation.TRANSCODE),
            ConversionPlanner.escalation(ConversionPlanner.Operation.REMUX_AUDIO),
        )
    }

    @Test
    fun transcodeSudahTitikAkhir() {
        assertTrue(ConversionPlanner.escalation(ConversionPlanner.Operation.TRANSCODE).isEmpty())
    }

    @Test
    fun urutanPercobaanSelaluDiawaliRencana() {
        ConversionPlanner.Operation.entries.forEach { operation ->
            assertEquals(operation, ConversionPlanner.attempts(operation).first())
        }
        assertEquals(
            listOf(
                ConversionPlanner.Operation.REMUX_AUDIO,
                ConversionPlanner.Operation.TRANSCODE,
            ),
            ConversionPlanner.attempts(ConversionPlanner.Operation.REMUX_AUDIO),
        )
    }

    @Test
    fun operasiTidakPernahDiulangDalamSatuTangga() {
        ConversionPlanner.Operation.entries.forEach { operation ->
            val attempts = ConversionPlanner.attempts(operation)
            assertEquals(attempts.size, attempts.toSet().size)
        }
    }

    // --- Validator MP4 ---

    @Test
    fun mp4KompatibelMenerimaH264Aac() {
        assertTrue(ConversionPlanner.isMp4Compatible("h264", "aac"))
        assertTrue(ConversionPlanner.isMp4Compatible("hevc", "mp3"))
    }

    @Test
    fun mp4KompatibelMenolakOpusDanVorbis() {
        assertFalse(ConversionPlanner.isMp4Compatible("h264", "opus"))
        assertFalse(ConversionPlanner.isMp4Compatible("h264", "vorbis"))
    }

    @Test
    fun mp4KompatibelMenolakVp8() {
        assertFalse(ConversionPlanner.isMp4Compatible("vp8", "aac"))
    }

    @Test
    fun mp4KompatibelMenerimaCodecBelumDiketahui() {
        assertTrue(ConversionPlanner.isMp4Compatible(null, null))
        assertTrue(ConversionPlanner.isMp4Compatible("h264", null))
    }

    // --- Label log/riwayat tidak berubah dari sebelumnya ---

    @Test
    fun labelModeTetapSamaDenganPerilakuLama() {
        assertEquals("native", ConversionPlanner.modeLabel(ConversionPlanner.Operation.NATIVE))
        assertEquals("copy", ConversionPlanner.modeLabel(ConversionPlanner.Operation.REMUX))
        assertEquals("remux", ConversionPlanner.modeLabel(ConversionPlanner.Operation.REMUX_AUDIO))
        assertEquals("encode", ConversionPlanner.modeLabel(ConversionPlanner.Operation.TRANSCODE))
    }
}
