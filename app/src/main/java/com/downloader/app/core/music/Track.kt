package com.downloader.app.core.music

data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val dateAddedMs: Long,
    val artworkUri: String?,
    val embeddedArt: ByteArray?,
    val isOwnDownload: Boolean,
    val playable: Boolean,
    /** Nama folder nyata tempat berkas berada, diambil dari lokasi berkas/MediaStore. */
    val folder: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
