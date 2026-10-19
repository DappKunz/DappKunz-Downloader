package com.downloader.app.core.music

import java.util.Locale

/** Album yang dibentuk dari metadata nyata hasil pemindaian. */
data class AlbumGroup(
    val key: String,
    val title: String,
    val artist: String?,
    val tracks: List<Track>,
) {
    /** Lagu yang paling mungkin punya sampul, dipakai sebagai wakil album. */
    val sample: Track
        get() = tracks.firstOrNull { it.artworkUri != null || it.embeddedArt != null } ?: tracks.first()
}

data class ArtistGroup(
    val key: String,
    val name: String,
    val tracks: List<Track>,
) {
    val sample: Track
        get() = tracks.firstOrNull { it.artworkUri != null || it.embeddedArt != null } ?: tracks.first()
}

data class FolderGroup(
    val key: String,
    val name: String,
    val tracks: List<Track>,
)

/**
 * Pengelompokan library. Objek ini murni (tanpa Android) supaya bisa diuji di JVM.
 *
 * Semua kelompok dibentuk dari metadata/lokasi berkas yang benar-benar ada pada hasil
 * pemindaian; tidak ada nama folder atau album yang dikarang dari nama berkas.
 */
object LibraryGroups {

    fun albums(tracks: List<Track>, unknownAlbum: String): List<AlbumGroup> =
        bucket(tracks, { it.album }, unknownAlbum)
            .map { (key, items) ->
                AlbumGroup(
                    key = key,
                    title = labelOf(items, unknownAlbum) { it.album },
                    artist = sharedArtist(items),
                    tracks = items,
                )
            }
            .sortedWith(byTitle(unknownAlbum) { it.title })

    fun artists(tracks: List<Track>, unknownArtist: String): List<ArtistGroup> =
        bucket(tracks, { it.artist }, unknownArtist)
            .map { (key, items) ->
                ArtistGroup(
                    key = key,
                    name = labelOf(items, unknownArtist) { it.artist },
                    tracks = items,
                )
            }
            .sortedWith(byTitle(unknownArtist) { it.name })

    fun folders(tracks: List<Track>, unknownFolder: String): List<FolderGroup> =
        bucket(tracks, { it.folder }, unknownFolder)
            .map { (key, items) ->
                FolderGroup(
                    key = key,
                    name = labelOf(items, unknownFolder) { it.folder },
                    tracks = items,
                )
            }
            .sortedWith(byTitle(unknownFolder) { it.name })

    /**
     * Nama folder dari lokasi berkas nyata.
     *
     * [legacyFilePath] true untuk kolom DATA MediaStore (path berkas penuh), false untuk
     * RELATIVE_PATH (folder relatif, mis. "Download/Folder/").
     */
    fun folderName(path: String?, legacyFilePath: Boolean): String? {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val name = if (legacyFilePath) {
            raw.substringBeforeLast('/', "").substringAfterLast('/')
        } else {
            raw.trim('/').substringAfterLast('/')
        }
        return name.trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Urutkan lagu sesuai urutan id yang tersimpan di playlist.
     *
     * Id yang berkasnya sudah hilang akan dilewati tanpa error.
     */
    fun resolveTracks(ids: List<String>, tracks: List<Track>): List<Track> {
        if (ids.isEmpty() || tracks.isEmpty()) return emptyList()
        val byId = tracks.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    /** Berapa id playlist yang tidak lagi punya berkas. */
    fun missingCount(ids: List<String>, tracks: List<Track>): Int {
        if (ids.isEmpty()) return 0
        val known = tracks.mapTo(HashSet()) { it.id }
        return ids.count { it !in known }
    }

    private fun bucket(
        tracks: List<Track>,
        value: (Track) -> String?,
        unknown: String,
    ): Map<String, List<Track>> {
        val buckets = LinkedHashMap<String, MutableList<Track>>()
        tracks.forEach { track ->
            val label = value(track)?.trim()?.takeIf { it.isNotEmpty() } ?: unknown
            buckets.getOrPut(label.lowercase(Locale.ROOT)) { mutableListOf() }.add(track)
        }
        return buckets
    }

    private fun labelOf(items: List<Track>, unknown: String, value: (Track) -> String?): String =
        items.firstNotNullOfOrNull { value(it)?.trim()?.takeIf { it.isNotEmpty() } } ?: unknown

    private fun sharedArtist(items: List<Track>): String? =
        items.mapNotNull { it.artist?.trim()?.takeIf { it.isNotEmpty() } }.distinct().singleOrNull()

    private fun <T> byTitle(unknown: String, title: (T) -> String): Comparator<T> =
        compareBy({ title(it).equals(unknown, ignoreCase = true) }, { title(it).lowercase(Locale.ROOT) })
}
