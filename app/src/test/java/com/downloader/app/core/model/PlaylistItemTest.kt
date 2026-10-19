package com.downloader.app.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Antrean yang sudah tersimpan di perangkat berisi [PlaylistItem] versi lama
 * (tanpa `artist`). Menambah field baru tidak boleh membuat antrean itu gagal
 * dibaca kembali.
 */
class PlaylistItemTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun itemLamaTanpaArtistTetapTerbaca() {
        val stored = """
            {"index":3,"id":"abc","title":"Baby","duration":213,
             "thumbnail":"https://i.ytimg.com/vi/x/hq.jpg","url":"https://youtu.be/x"}
        """.trimIndent()
        val item = json.decodeFromString(PlaylistItem.serializer(), stored)
        assertEquals(3, item.index)
        assertEquals("Baby", item.title)
        assertNull(item.artist)
    }

    @Test
    fun artistBertahanSetelahPutaranSerialisasi() {
        val item = PlaylistItem(index = 1, id = "a", title = "Baby", artist = "Justin Bieber")
        val decoded = json.decodeFromString(
            PlaylistItem.serializer(),
            json.encodeToString(PlaylistItem.serializer(), item),
        )
        assertEquals("Justin Bieber", decoded.artist)
    }
}
