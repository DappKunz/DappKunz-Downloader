package com.downloader.app.core.util

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileSignatureTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "sig-${System.nanoTime()}")
        dir.mkdirs()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun file(name: String, vararg bytes: Int): File =
        File(dir, name).apply { writeBytes(ByteArray(bytes.size) { bytes[it].toByte() }) }

    @Test
    fun acceptsMp3WithId3Tag() {
        val mp3 = file("a.mp3", 0x49, 0x44, 0x33, 4, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(FileSignature.hasValidMagic(mp3, "mp3"))
    }

    @Test
    fun acceptsMp3FrameSync() {
        val mp3 = file("b.mp3", 0xFF, 0xFB, 0x90, 0x64, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(FileSignature.hasValidMagic(mp3, "mp3"))
    }

    @Test
    fun rejectsMp3WithForeignHeader() {
        val fake = file("c.mp3", 0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0, 0, 0, 0)
        assertFalse(FileSignature.hasValidMagic(fake, "mp3"))
    }

    @Test
    fun acceptsMp4WithFtypBox() {
        val mp4 = file("a.mp4", 0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D)
        assertTrue(FileSignature.hasValidMagic(mp4, "mp4"))
        assertTrue(FileSignature.hasValidMagic(mp4, "m4a"))
    }

    @Test
    fun rejectsMp4WithoutFtyp() {
        val fake = file("b.mp4", 0x00, 0x00, 0x00, 0x20, 0x6D, 0x64, 0x61, 0x74, 0, 0, 0, 0)
        assertFalse(FileSignature.hasValidMagic(fake, "mp4"))
    }

    @Test
    fun rejectsTruncatedFiles() {
        val tiny = file("tiny.mp4", 0x00, 0x00, 0x00)
        assertFalse(FileSignature.hasValidMagic(tiny, "mp4"))
        assertNull(FileSignature.imageExtension(tiny))
    }

    @Test
    fun detectsImageTypeFromContent() {
        val jpg = file("photo.bin", 0xFF, 0xD8, 0xFF, 0xE0, 0, 0, 0, 0)
        val png = file("photo2.bin", 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0)
        val webp = file(
            "photo3.bin",
            0x52, 0x49, 0x46, 0x46, 0x10, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50,
        )
        val gif = file("photo4.bin", 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0, 0, 0, 0, 0, 0)

        assertEquals("jpg", FileSignature.imageExtension(jpg))
        assertEquals("png", FileSignature.imageExtension(png))
        assertEquals("webp", FileSignature.imageExtension(webp))
        assertEquals("gif", FileSignature.imageExtension(gif))
        assertTrue(FileSignature.hasValidImage(jpg))
    }

    @Test
    fun rejectsNonImageContent() {
        val text = file("not-image.jpg", 0x3C, 0x68, 0x74, 0x6D, 0x6C, 0x3E, 0, 0, 0, 0, 0, 0)
        assertNull(FileSignature.imageExtension(text))
        assertFalse(FileSignature.hasValidImage(text))
    }

    @Test
    fun unknownFormatFallsBackToOtherChecks() {
        val zip = file("a.zip", 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0)
        assertTrue(FileSignature.hasValidMagic(zip, "zip"))
    }
}
