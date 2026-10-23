package com.downloader.app.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {

    @Test
    fun normalizesHexWithOptionalHash() {
        assertEquals("#3B82F6", normalizeHexColor("3b82f6"))
        assertEquals("#3B82F6", normalizeHexColor(" #3B82f6 "))
        assertEquals("#000000", normalizeHexColor("#000000"))
        assertEquals("#FFFFFF", normalizeHexColor("#ffffff"))
    }

    @Test
    fun rejectsMalformedHex() {
        assertNull(normalizeHexColor(""))
        assertNull(normalizeHexColor("   "))
        assertNull(normalizeHexColor("#3B82F"))
        assertNull(normalizeHexColor("#3B82F6A"))
        assertNull(normalizeHexColor("#3B82FG"))
        assertNull(normalizeHexColor("##3B82F6"))
        assertNull(normalizeHexColor("blue"))
    }

    @Test
    fun isHexColorMirrorsNormalization() {
        assertTrue(isHexColor("3B82F6"))
        assertFalse(isHexColor("#12345"))
        assertFalse(isHexColor(""))
    }

    @Test
    fun enumKeysRoundTrip() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey(ThemeMode.LIGHT.key))
        assertEquals(AccentPalette.GREEN, AccentPalette.fromKey(AccentPalette.GREEN.key))
        assertEquals(FontChoice.CUSTOM, FontChoice.fromKey(FontChoice.CUSTOM.key))
        assertEquals("#4BF63B", AccentPalette.GREEN.hex)
        assertNull(AccentPalette.CUSTOM.hex)
    }

    @Test
    fun unknownKeysFallBackToDefaults() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("sepia"))
        assertEquals(AccentPalette.BLUE, AccentPalette.fromKey("purple"))
        assertEquals(FontChoice.DEFAULT, FontChoice.fromKey("comic_sans"))
    }
}
