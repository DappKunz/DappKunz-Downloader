package com.downloader.app.core.music

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Playlist buatan pengguna. Hanya menyimpan id lagu, bukan berkasnya, supaya
 * playlist tidak ikut rusak kalau berkas dipindah atau dihapus.
 */
@Serializable
data class MusicPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
)

/**
 * Penyimpanan playlist dalam bentuk teks JSON.
 *
 * Seluruh operasi di sini murni (tanpa Android) supaya bisa diuji di JVM; lapisan
 * penyimpanan cukup memanggil [encode]/[decode].
 */
object PlaylistStore {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(raw: String?): List<MusicPlaylist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<MusicPlaylist>>(raw) }
            .getOrDefault(emptyList())
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
    }

    fun encode(playlists: List<MusicPlaylist>): String =
        runCatching { json.encodeToString(playlists) }.getOrDefault("[]")

    fun newId(existing: List<MusicPlaylist>, seed: Long): String {
        val base = "pl-$seed"
        if (existing.none { it.id == base }) return base
        var suffix = 2
        while (existing.any { it.id == "$base-$suffix" }) suffix++
        return "$base-$suffix"
    }

    fun create(
        current: List<MusicPlaylist>,
        name: String,
        id: String,
        createdAt: Long,
    ): List<MusicPlaylist> {
        val clean = name.trim()
        if (clean.isEmpty() || id.isBlank()) return current
        return current + MusicPlaylist(id = id, name = clean, createdAt = createdAt)
    }

    fun rename(current: List<MusicPlaylist>, id: String, name: String): List<MusicPlaylist> {
        val clean = name.trim()
        if (clean.isEmpty()) return current
        return current.map { if (it.id == id) it.copy(name = clean) else it }
    }

    fun delete(current: List<MusicPlaylist>, id: String): List<MusicPlaylist> =
        current.filterNot { it.id == id }

    fun addTrack(current: List<MusicPlaylist>, playlistId: String, trackId: String): List<MusicPlaylist> {
        if (trackId.isBlank()) return current
        return current.map { playlist ->
            if (playlist.id != playlistId || trackId in playlist.trackIds) {
                playlist
            } else {
                playlist.copy(trackIds = playlist.trackIds + trackId)
            }
        }
    }

    fun removeTrack(current: List<MusicPlaylist>, playlistId: String, trackId: String): List<MusicPlaylist> =
        current.map { playlist ->
            if (playlist.id != playlistId) {
                playlist
            } else {
                playlist.copy(trackIds = playlist.trackIds.filterNot { it == trackId })
            }
        }
}
