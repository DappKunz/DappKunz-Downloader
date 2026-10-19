package com.downloader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaProbeTest {

    private val videoJson = """
        {
          "streams": [
            {"codec_type": "video", "codec_name": "h264", "duration": "12.480000"},
            {"codec_type": "audio", "codec_name": "aac", "duration": "12.480000"}
          ],
          "format": {"duration": "12.500000", "format_name": "mov,mp4,m4a,3gp,3g2,mj2"}
        }
    """.trimIndent()

    private val audioJson = """
        {
          "streams": [{"codec_type": "audio", "codec_name": "mp3", "duration": "180.000000"}],
          "format": {"duration": "180.020000", "format_name": "mp3"}
        }
    """.trimIndent()

    @Test
    fun parsesVideoProbe() {
        val info = MediaProbe.parse(videoJson)
        assertEquals(listOf("video", "audio"), info?.streamTypes)
        assertEquals(12.5, info?.durationSeconds ?: 0.0, 0.001)
        assertTrue(MediaProbe.isValid(info, MediaProbe.Expect.VIDEO))
    }

    @Test
    fun rejectsAudioOnlyForVideoExpectation() {
        val info = MediaProbe.parse(audioJson)
        assertTrue(MediaProbe.isValid(info, MediaProbe.Expect.AUDIO))
        assertFalse(MediaProbe.isValid(info, MediaProbe.Expect.VIDEO))
    }

    @Test
    fun requiresPositiveDuration() {
        val json = """
            {"streams": [{"codec_type": "audio"}], "format": {"duration": "0.000000"}}
        """.trimIndent()
        assertFalse(MediaProbe.isValid(MediaProbe.parse(json), MediaProbe.Expect.AUDIO))
    }

    @Test
    fun readsDurationFromStreamWhenFormatMissing() {
        val json = """
            {"streams": [{"codec_type": "video", "duration": "31.250000"}]}
        """.trimIndent()
        val info = MediaProbe.parse(json)
        assertEquals(31.25, info?.durationSeconds ?: 0.0, 0.001)
        assertTrue(MediaProbe.isValid(info, MediaProbe.Expect.VIDEO))
    }

    @Test
    fun rejectsMalformedOrEmptyProbe() {
        assertNull(MediaProbe.parse("not json"))
        assertNull(MediaProbe.parse("""{"streams": []}"""))
        assertFalse(MediaProbe.isValid(null, MediaProbe.Expect.AUDIO))
    }

    @Test
    fun ignoresLogNoiseAroundJson() {
        val noisy = "ffprobe version 6.0\n$audioJson\ntrailing warning"
        assertTrue(MediaProbe.isValid(MediaProbe.parse(noisy), MediaProbe.Expect.AUDIO))
    }

    @Test
    fun membacaCodecPerStreamUntukPerencanaanOperasi() {
        val info = MediaProbe.parse(videoJson)
        assertEquals("h264", info?.videoCodec())
        assertEquals("aac", info?.audioCodec())
        assertFalse(info?.hasAttachedPicture() ?: true)
        assertEquals(2, info?.streams?.size)
    }

    @Test
    fun streamTypesTetapKonsistenDenganStreams() {
        val info = MediaProbe.parse(videoJson)
        assertEquals(info?.streams?.map { it.type }, info?.streamTypes)
    }

    @Test
    fun coverTertanamBukanVideoDanTerdeteksi() {
        // m4a dengan sampul: tanpa penyaringan attached_pic, berkas audio ini
        // akan terlihat seperti "punya video" dan cover-nya di-embed dua kali.
        val json = """
            {
              "streams": [
                {"codec_type": "video", "codec_name": "mjpeg", "disposition": {"attached_pic": 1}},
                {"codec_type": "audio", "codec_name": "mp3", "duration": "200.000000"}
              ],
              "format": {"duration": "200.000000", "format_name": "mp3"}
            }
        """.trimIndent()
        val info = MediaProbe.parse(json)
        assertNull(info?.videoCodec())
        assertEquals("mp3", info?.audioCodec())
        assertTrue(info?.hasAttachedPicture() ?: false)
    }

    @Test
    fun audioTanpaCoverTidakDianggapPunyaCover() {
        val info = MediaProbe.parse(audioJson)
        assertFalse(info?.hasAttachedPicture() ?: true)
    }

    @Test
    fun streamTanpaCodecNameTidakMenggagalkanParse() {
        val json = """
            {"streams": [{"codec_type": "audio", "duration": "5.000000"}], "format": {"duration": "5.000000"}}
        """.trimIndent()
        val info = MediaProbe.parse(json)
        assertEquals(listOf("audio"), info?.streamTypes)
        assertNull(info?.audioCodec())
        assertTrue(MediaProbe.isValid(info, MediaProbe.Expect.AUDIO))
    }

    @Test
    fun membacaTagKontainerTanpaProsesTambahan() {
        // Tag dipakai untuk memutuskan apakah berkas masih perlu ditulis ulang;
        // datanya datang dari ffprobe yang memang sudah dijalankan.
        val json = """
            {
              "streams": [{"codec_type": "audio", "codec_name": "mp3", "duration": "180.000000"}],
              "format": {
                "duration": "180.020000",
                "format_name": "mp3",
                "tags": {"ARTIST": "Justin Bieber", "Title": "Baby", "album": "My World 2.0"}
              }
            }
        """.trimIndent()
        val info = MediaProbe.parse(json)
        assertEquals("Justin Bieber", info?.tag("artist"))
        assertEquals("Baby", info?.tag("title"))
        assertEquals("My World 2.0", info?.tag("album"))
    }

    @Test
    fun tagTidakAdaMenghasilkanNull() {
        assertNull(MediaProbe.parse(audioJson)?.tag("artist"))
    }

    @Test
    fun tagKosongDiabaikan() {
        val json = """
            {
              "streams": [{"codec_type": "audio", "codec_name": "mp3", "duration": "10.000000"}],
              "format": {"duration": "10.000000", "tags": {"artist": "   "}}
            }
        """.trimIndent()
        assertNull(MediaProbe.parse(json)?.tag("artist"))
    }

    @Test
    fun tagBernilaiBukanStringTidakMenggagalkanParse() {
        val json = """
            {
              "streams": [{"codec_type": "audio", "codec_name": "mp3", "duration": "10.000000"}],
              "format": {"duration": "10.000000", "tags": {"artist": "Justin Bieber", "aneh": {"a": 1}}}
            }
        """.trimIndent()
        assertEquals("Justin Bieber", MediaProbe.parse(json)?.tag("artist"))
    }
}
