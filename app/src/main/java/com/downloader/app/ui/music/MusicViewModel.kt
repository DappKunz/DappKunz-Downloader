package com.downloader.app.ui.music

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.downloader.app.core.music.MusicController
import com.downloader.app.core.music.MusicLibrary
import com.downloader.app.core.music.MusicSort
import com.downloader.app.core.music.Track
import com.downloader.app.core.storage.PrefsRepository

/** Kategori yang bisa ditelusuri di Music Library. */
enum class LibraryCategory { SONGS, ALBUMS, ARTISTS, FOLDERS, FAVORITES, PLAYLISTS }

/** Layar turunan di dalam Music Library (album/artis/folder/playlist yang dibuka). */
sealed interface LibraryDetail {
    data class Album(val key: String) : LibraryDetail
    data class Artist(val key: String) : LibraryDetail
    data class Folder(val key: String) : LibraryDetail
    data class Playlist(val id: String) : LibraryDetail
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    var query by mutableStateOf("")
        private set

    var sort by mutableStateOf(MusicSort.RECENT)
        private set

    var category by mutableStateOf(LibraryCategory.SONGS)
        private set

    var detail by mutableStateOf<LibraryDetail?>(null)
        private set

    fun onQueryChange(value: String) {
        query = value
    }

    fun onSortChange(value: MusicSort) {
        sort = value
    }

    fun onCategoryChange(value: LibraryCategory) {
        category = value
        detail = null
    }

    fun openDetail(value: LibraryDetail) {
        detail = value
    }

    fun closeDetail() {
        detail = null
    }

    fun toggleFavorite(id: String) {
        PrefsRepository.toggleFavoriteTrack(id)
    }

    fun refresh() {
        MusicLibrary.refresh(getApplication())
    }

    fun updatePermission() {
        MusicLibrary.updatePermission(getApplication())
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            MusicLibrary.updatePermission(getApplication())
            MusicLibrary.refresh(getApplication())
        } else {
            MusicLibrary.updatePermission(getApplication())
        }
    }

    fun playTrack(track: Track, visible: List<Track>) {
        val index = visible.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: return
        MusicController.playQueue(getApplication(), visible, index)
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        MusicController.playQueue(getApplication(), tracks, 0)
    }

    fun shuffleAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        MusicController.playQueue(getApplication(), tracks, 0)
        if (!MusicController.shuffle.value) MusicController.toggleShuffle()
    }

    fun playNext(track: Track) {
        MusicController.playNext(getApplication(), track)
    }

    fun addToQueue(track: Track) {
        MusicController.addToQueue(getApplication(), track)
    }

    fun createPlaylist(name: String): String? = PrefsRepository.createPlaylist(name)

    fun renamePlaylist(id: String, name: String) = PrefsRepository.renamePlaylist(id, name)

    fun deletePlaylist(id: String) {
        PrefsRepository.deletePlaylist(id)
        if ((detail as? LibraryDetail.Playlist)?.id == id) detail = null
    }

    /** true bila lagu benar-benar ditambahkan, false bila sudah ada di playlist itu. */
    fun addToPlaylist(playlistId: String, trackId: String): Boolean =
        PrefsRepository.addTrackToPlaylist(playlistId, trackId)

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        PrefsRepository.removeTrackFromPlaylist(playlistId, trackId)
    }
}
