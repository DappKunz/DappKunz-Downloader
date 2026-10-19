package com.downloader.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceInfo(
    val url: String,
    val platform: String = "unknown",
)

@Serializable
data class MediaInfo(
    val type: String = "video",
    val isPlaylist: Boolean = false,
    val title: String = "Untitled Media",
    val creator: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null,
)

@Serializable
data class Capabilities(
    val audio: Boolean = false,
    val video: Boolean = false,
    val photo: Boolean = false,
    val mp3: Boolean = false,
    val m4a: Boolean = false,
    val mp4: Boolean = false,
    val image: Boolean = false,
)

@Serializable
data class AudioFormat(
    val codec: String? = null,
    val container: String? = null,
    val bitrate: Int? = null,
)

@Serializable
data class VideoFormat(
    val container: String? = null,
    val height: Int? = null,
    val fps: Float? = null,
)

@Serializable
data class Formats(
    val audio: List<AudioFormat> = emptyList(),
    val video: List<VideoFormat> = emptyList(),
)

@Serializable
data class PlaylistItem(
    val index: Int,
    val id: String,
    val title: String,
    val duration: Int? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    /**
     * Artis/kanal menurut **entri playlist itu sendiri**, bukan pemilik playlist.
     * Dibiarkan null bila extractor tidak menyediakannya; nilai null jauh lebih
     * benar daripada memakai nama pemilik playlist (mis. lagu Justin Bieber
     * diberi artis nama pembuat playlist).
     */
    val artist: String? = null,
)

@Serializable
data class PlaylistInfo(
    val itemCount: Int = 0,
    val title: String? = null,
    val totalDuration: Int? = null,
    val items: List<PlaylistItem> = emptyList(),
)

@Serializable
data class NormalizedMetadata(
    val source: SourceInfo,
    val media: MediaInfo,
    val capabilities: Capabilities,
    val formats: Formats,
    val playlist: PlaylistInfo? = null,
    val photoImages: List<String> = emptyList(),
)
