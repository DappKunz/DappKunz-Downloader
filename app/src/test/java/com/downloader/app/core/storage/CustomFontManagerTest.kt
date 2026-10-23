package com.downloader.app.core.storage

import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomFontManagerTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "font-sig-${System.nanoTime()}")
        dir.mkdirs()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun file(name: String, vararg bytes: Int): File =
        File(dir, name).apply { writeBytes(ByteArray(bytes.size) { bytes[it].toByte() }) }

    @Test
    fun acceptsTrueTypeSignature() {
        val font = file("font.ttf", 0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(CustomFontManager.hasValidFontSignature(font))
    }

    @Test
    fun acceptsOpenTypeAndCollectionSignatures() {
        val openType = file("font.otf", 0x4F, 0x54, 0x54, 0x4F, 0, 0, 0, 0, 0, 0, 0, 0)
        val trueType = file("font-true.ttf", 0x74, 0x72, 0x75, 0x65, 0, 0, 0, 0, 0, 0, 0, 0)
        val typeOne = file("font.typ1", 0x74, 0x79, 0x70, 0x31, 0, 0, 0, 0, 0, 0, 0, 0)
        val collection = file("font.ttc", 0x74, 0x74, 0x63, 0x66, 0, 0, 0, 0, 0, 0, 0, 0)

        assertTrue(CustomFontManager.hasValidFontSignature(openType))
        assertTrue(CustomFontManager.hasValidFontSignature(trueType))
        assertTrue(CustomFontManager.hasValidFontSignature(typeOne))
        assertTrue(CustomFontManager.hasValidFontSignature(collection))
    }

    @Test
    fun rejectsForeignAndTruncatedFiles() {
        val webFont = file("font.woff", 0x77, 0x4F, 0x46, 0x46, 0, 0, 0, 0, 0, 0, 0, 0)
        val text = file("font.txt", 0x68, 0x65, 0x6C, 0x6C, 0x6F, 0, 0, 0, 0, 0, 0, 0)
        val tiny = file("tiny.ttf", 0x00, 0x01)
        val missing = File(dir, "missing.ttf")

        assertFalse(CustomFontManager.hasValidFontSignature(webFont))
        assertFalse(CustomFontManager.hasValidFontSignature(text))
        assertFalse(CustomFontManager.hasValidFontSignature(tiny))
        assertFalse(CustomFontManager.hasValidFontSignature(missing))
    }
}
