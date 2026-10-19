package com.downloader.app.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupsTest {

    private fun track(
        id: String,
        title: String = "Lagu $id",
        artist: String? = null,
        album: String? = null,
        folder: String? = null,
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
        folder = folder,
    )

    @Test
    fun albumsAreGroupedIgnoringCaseAndUnknownIsLast() {
        val tracks = listOf(
            track("1", album = "Rumah"),
            track("2", album = "rumah"),
            track("3", album = null),
            track("4", album = "   "),
            track("5", album = "Angkasa"),
        )

        val albums = LibraryGroups.albums(tracks, "Album tidak dikenal")

        assertEquals(listOf("Angkasa", "Rumah", "Album tidak dikenal"), albums.map { it.title })
        assertEquals(2, albums.first { it.title == "Rumah" }.tracks.size)
        assertEquals(2, albums.last().tracks.size)
    }

    @Test
    fun albumArtistIsOnlyShownWhenAllTracksShareIt() {
        val single = LibraryGroups.albums(
            listOf(track("1", album = "Rumah", artist = "Dapp"), track("2", album = "Rumah", artist = "Dapp")),
            "Tidak dikenal",
        )
        assertEquals("Dapp", single.single().artist)

        val mixed = LibraryGroups.albums(
            listOf(track("1", album = "Rumah", artist = "Dapp"), track("2", album = "Rumah", artist = "Kunz")),
            "Tidak dikenal",
        )
        assertNull(mixed.single().artist)
    }

    @Test
    fun artistsAreGroupedAndSortedWithUnknownLast() {
        val tracks = listOf(
            track("1", artist = "Budi"),
            track("2", artist = "budi"),
            track("3", artist = null),
            track("4", artist = "Ani"),
        )

        val artists = LibraryGroups.artists(tracks, "Artis tidak dikenal")

        assertEquals(listOf("Ani", "Budi", "Artis tidak dikenal"), artists.map { it.name })
        assertEquals(2, artists.first { it.name == "Budi" }.tracks.size)
    }

    @Test
    fun foldersComeFromRealLocationsOnly() {
        val tracks = listOf(
            track("1", folder = "Download"),
            track("2", folder = null),
            track("3", folder = "Music"),
        )

        val folders = LibraryGroups.folders(tracks, "Folder tidak dikenal")

        assertEquals(listOf("Download", "Music", "Folder tidak dikenal"), folders.map { it.name })
        assertEquals("3", folders.first { it.name == "Music" }.tracks.single().id)
    }

    @Test
    fun folderNameHandlesRelativeAndLegacyPaths() {
        // RELATIVE_PATH (Android 10+)
        assertEquals("DappKunzXD Downloader", LibraryGroups.folderName("Download/DappKunzXD Downloader/", false))
        assertEquals("Music", LibraryGroups.folderName("/storage/emulated/0/Music", false))

        // DATA berupa path berkas penuh (Android 9 ke bawah)
        assertEquals("Music", LibraryGroups.folderName("/storage/emulated/0/Music/song.mp3", true))
        assertEquals("Download", LibraryGroups.folderName("/sdcard/Download/song.mp3", true))

        assertNull(LibraryGroups.folderName("", false))
        assertNull(LibraryGroups.folderName("   ", true))
        assertNull(LibraryGroups.folderName(null, false))
        assertNull(LibraryGroups.folderName("/song.mp3", true))
        assertEquals("Download", LibraryGroups.folderName("Download/", false))
    }

    @Test
    fun resolveTracksKeepsPlaylistOrderAndSkipsMissingFiles() {
        val tracks = listOf(track("1"), track("2"), track("3"))
        val resolved = LibraryGroups.resolveTracks(listOf("3", "hilang", "1"), tracks)
        assertEquals(listOf("3", "1"), resolved.map { it.id })
        assertEquals(1, LibraryGroups.missingCount(listOf("3", "hilang", "1"), tracks))
        assertTrue(LibraryGroups.resolveTracks(emptyList(), tracks).isEmpty())
        assertTrue(LibraryGroups.resolveTracks(listOf("1"), emptyList()).isEmpty())
    }
}
