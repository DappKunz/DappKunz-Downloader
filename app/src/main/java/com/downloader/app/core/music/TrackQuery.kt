package com.downloader.app.core.music

import java.util.Locale

/** Urutan tampilan daftar lagu. */
enum class MusicSort { RECENT, NAME, ARTIST, ALBUM }

/**
 * Pencarian dan pengurutan daftar lagu.
 *
 * Objek ini murni (tanpa Android) supaya aturannya bisa diuji di JVM dan supaya
 * pencarian judul/artis/album tidak ditulis ulang di beberapa layar.
 */
object TrackQuery {

    private const val LAST = "\uFFFF"

    fun filterQuery(tracks: List<Track>, query: String): List<Track> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return tracks
        return tracks.filter { track ->
            track.title.lowercase(Locale.ROOT).contains(q) ||
                track.artist?.lowercase(Locale.ROOT)?.contains(q) == true ||
                track.album?.lowercase(Locale.ROOT)?.contains(q) == true
        }
    }

    fun sort(tracks: List<Track>, sort: MusicSort): List<Track> = when (sort) {
        MusicSort.NAME -> tracks.sortedBy { it.title.lowercase(Locale.ROOT) }
        MusicSort.ARTIST -> tracks.sortedBy { (it.artist ?: LAST).lowercase(Locale.ROOT) }
        MusicSort.ALBUM -> tracks.sortedWith(
            compareBy(
                { (it.album ?: LAST).lowercase(Locale.ROOT) },
                { it.title.lowercase(Locale.ROOT) },
            ),
        )

        MusicSort.RECENT -> tracks.sortedByDescending { it.dateAddedMs }
    }

    fun filterSort(tracks: List<Track>, query: String, sort: MusicSort): List<Track> =
        sort(filterQuery(tracks, query), sort)
}
