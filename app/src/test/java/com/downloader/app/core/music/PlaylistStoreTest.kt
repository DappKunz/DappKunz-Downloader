package com.downloader.app.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistStoreTest {

    @Test
    fun createTrimsNameAndIgnoresBlankNames() {
        val created = PlaylistStore.create(emptyList(), "  Ngebut  ", "pl-1", 10L)
        assertEquals(1, created.size)
        assertEquals("Ngebut", created.single().name)
        assertEquals("pl-1", created.single().id)

        assertEquals(0, PlaylistStore.create(emptyList(), "   ", "pl-2", 10L).size)
        assertEquals(0, PlaylistStore.create(emptyList(), "Nama", "  ", 10L).size)
    }

    @Test
    fun newIdNeverCollides() {
        val first = PlaylistStore.newId(emptyList(), 100L)
        val second = PlaylistStore.newId(PlaylistStore.create(emptyList(), "A", first, 1L), 100L)
        assertEquals("pl-100", first)
        assertFalse(first == second)
        assertEquals("pl-100-2", second)
    }

    @Test
    fun addTrackIsIdempotent() {
        val base = PlaylistStore.create(emptyList(), "Favorit", "pl-1", 1L)
        val once = PlaylistStore.addTrack(base, "pl-1", "track-1")
        assertEquals(listOf("track-1"), once.single().trackIds)

        val twice = PlaylistStore.addTrack(once, "pl-1", "track-1")
        assertEquals(once, twice)
        assertEquals(1, twice.single().trackIds.size)

        // Menambah ke playlist lain tidak menyentuh playlist pertama.
        val withSecond = PlaylistStore.create(once, "Kedua", "pl-2", 2L)
        val added = PlaylistStore.addTrack(withSecond, "pl-2", "track-2")
        assertEquals(listOf("track-1"), added.first { it.id == "pl-1" }.trackIds)
        assertEquals(listOf("track-2"), added.first { it.id == "pl-2" }.trackIds)
    }

    @Test
    fun removeTrackAndRenameAndDelete() {
        var list = PlaylistStore.create(emptyList(), "Lama", "pl-1", 1L)
        list = PlaylistStore.addTrack(list, "pl-1", "a")
        list = PlaylistStore.addTrack(list, "pl-1", "b")

        list = PlaylistStore.removeTrack(list, "pl-1", "a")
        assertEquals(listOf("b"), list.single().trackIds)

        list = PlaylistStore.rename(list, "pl-1", "  Baru ")
        assertEquals("Baru", list.single().name)

        // Nama kosong tidak mengubah apa pun.
        assertEquals("Baru", PlaylistStore.rename(list, "pl-1", "  ").single().name)

        assertTrue(PlaylistStore.delete(list, "pl-1").isEmpty())
    }

    @Test
    fun encodeDecodeRoundTrip() {
        var list = PlaylistStore.create(emptyList(), "Road trip", "pl-1", 5L)
        list = PlaylistStore.addTrack(list, "pl-1", "track-1")
        list = PlaylistStore.addTrack(list, "pl-1", "track-2")

        val raw = PlaylistStore.encode(list)
        assertEquals(list, PlaylistStore.decode(raw))
    }

    @Test
    fun decodeIsForgiving() {
        assertTrue(PlaylistStore.decode(null).isEmpty())
        assertTrue(PlaylistStore.decode("").isEmpty())
        assertTrue(PlaylistStore.decode("{bukan json}").isEmpty())

        // Entri tanpa id/nama dibuang, sisanya tetap dibaca.
        val raw = """
            [
              {"id":"pl-1","name":"Valid","trackIds":["a"]},
              {"id":"","name":"Tanpa id"},
              {"id":"pl-3","name":"   "}
            ]
        """.trimIndent()
        val decoded = PlaylistStore.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("pl-1", decoded.single().id)
    }
}
