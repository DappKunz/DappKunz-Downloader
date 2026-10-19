package com.downloader.app.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackQueryTest {

    private fun track(
        id: String,
        title: String,
        artist: String? = null,
        album: String? = null,
        dateAdded: Long = 0L,
    ) = Track(
        id = id,
        uri = "content://media/$id",
        title = title,
        artist = artist,
        album = album,
        durationMs = 1000L,
        dateAddedMs = dateAdded,
        artworkUri = null,
        embeddedArt = null,
        isOwnDownload = false,
        playable = true,
    )

    private val library = listOf(
        track("1", title = "Senja", artist = "Budi", album = "Langit", dateAdded = 30L),
        track("2", title = "Pagi", artist = "Ani", album = "Bumi", dateAdded = 10L),
        track("3", title = "Malam", artist = "Budi", album = "Langit", dateAdded = 20L),
    )

    @Test
    fun searchMatchesTitleArtistAndAlbum() {
        assertEquals(listOf("1"), TrackQuery.filterQuery(library, "senja").map { it.id })
        assertEquals(listOf("1", "3"), TrackQuery.filterQuery(library, "budi").map { it.id })
        assertEquals(listOf("2"), TrackQuery.filterQuery(library, "bumi").map { it.id })
        assertEquals(listOf("1", "2", "3"), TrackQuery.filterQuery(library, "  ").map { it.id })
        assertTrue(TrackQuery.filterQuery(library, "tidak ada").isEmpty())
    }

    @Test
    fun sortsByNameArtistAlbumAndRecent() {
        assertEquals(listOf("3", "2", "1"), TrackQuery.sort(library, MusicSort.NAME).map { it.id })
        assertEquals(listOf("2", "1", "3"), TrackQuery.sort(library, MusicSort.ARTIST).map { it.id })
        assertEquals(listOf("2", "3", "1"), TrackQuery.sort(library, MusicSort.ALBUM).map { it.id })
        assertEquals(listOf("1", "3", "2"), TrackQuery.sort(library, MusicSort.RECENT).map { it.id })
    }

    @Test
    fun tracksWithoutArtistOrAlbumGoLastInSort() {
        val list = library + track("4", title = "Tanpa metadata")
        assertEquals("4", TrackQuery.sort(list, MusicSort.ARTIST).last().id)
        assertEquals("4", TrackQuery.sort(list, MusicSort.ALBUM).last().id)
    }

    @Test
    fun filterThenSortCombinesBoth() {
        assertEquals(listOf("3", "1"), TrackQuery.filterSort(library, "budi", MusicSort.NAME).map { it.id })
        assertEquals(listOf("1", "3"), TrackQuery.filterSort(library, "budi", MusicSort.RECENT).map { it.id })
    }
}
