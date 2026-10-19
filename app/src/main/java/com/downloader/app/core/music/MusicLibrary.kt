package com.downloader.app.core.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.storage.OutputPublisher
import com.downloader.app.core.storage.PrefsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object MusicLibrary {

    sealed interface ScanState {
        data object Idle : ScanState
        data object Loading : ScanState
        data object Done : ScanState
        data class Failed(val message: String) : ScanState
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanMutex = Mutex()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    @Volatile
    private var loadedOnce = false

    fun audioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun updatePermission(context: Context) {
        _permissionGranted.value = ContextCompat.checkSelfPermission(
            context.applicationContext,
            audioPermission(),
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshIfLoaded(context: Context) {
        if (loadedOnce) refresh(context)
    }

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        updatePermission(appContext)
        scope.launch {
            scanMutex.withLock {
                _scanState.value = ScanState.Loading
                try {
                    _tracks.value = scanInternal(appContext)
                    loadedOnce = true
                    _scanState.value = ScanState.Done
                    AppLog.d("Music", "Library: ${_tracks.value.size} lagu terdeteksi")
                } catch (e: Exception) {
                    _scanState.value = ScanState.Failed(e.message ?: "scan failed")
                    AppLog.e("Music", "Scan gagal: ${e.message}")
                }
            }
        }
    }

    private suspend fun scanInternal(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val granted = _permissionGranted.value
        val found = LinkedHashMap<String, Track>()
        if (granted) {
            runCatching { queryMediaStore(context) }.getOrNull()?.forEach { track ->
                found.putIfAbsent(track.id, track)
            }
        } else {
            runCatching { queryOwnFolderFiles() }.getOrNull()?.forEach { track ->
                found.putIfAbsent(track.id, track)
            }
        }
        val knownNames = found.values.mapTo(LinkedHashSet()) { trackKey(it.title, it.durationMs) }
        runCatching { queryChosenFolder(context, knownNames) }.getOrNull()?.forEach { track ->
            found.putIfAbsent(track.id, track)
        }
        found.values.toList()
    }

    private fun trackKey(title: String, durationMs: Long): String =
        "${title.trim().lowercase()}|${durationMs / 1000}"

    @Suppress("DEPRECATION")
    private fun queryOwnFolderFiles(): List<Track> {
        val base = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            OutputPublisher.PUBLIC_FOLDER,
        )
        if (!base.isDirectory || !base.canRead()) return emptyList()
        val result = mutableListOf<Track>()
        runCatching {
            base.walkTopDown().maxDepth(3).filter { it.isFile }.take(MAX_TRACKS).forEach { file ->
                if (!isSupported(file.name, null)) return@forEach
                val meta = runCatching { readFileMetadata(file.absolutePath) }.getOrNull() ?: return@forEach
                val title = meta.title ?: file.nameWithoutExtension
                result.add(
                    Track(
                        id = "own:${file.absolutePath}",
                        uri = Uri.fromFile(file).toString(),
                        title = title,
                        artist = meta.artist,
                        album = meta.album,
                        durationMs = meta.durationMs,
                        dateAddedMs = file.lastModified(),
                        artworkUri = null,
                        embeddedArt = meta.art,
                        isOwnDownload = true,
                        playable = true,
                        folder = file.parentFile?.name ?: base.name,
                    ),
                )
            }
        }
        return result
    }

    private fun queryMediaStore(context: Context): List<Track> {
        val result = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.DATA)
        } else {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val pathColName = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.DATA
            } else {
                MediaStore.Audio.Media.RELATIVE_PATH
            }
            val pathCol = cursor.getColumnIndex(pathColName)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val durationMs = cursor.getLong(durationCol)
                if (durationMs <= 0) continue
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val path = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: uri.substringAfterLast('/').substringBefore('?')
                val rawArtist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() }
                val artist = if (rawArtist == "<unknown>") null else rawArtist
                val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                val mime = cursor.getString(mimeCol).orEmpty()
                val albumId = cursor.getLong(albumIdCol)
                val artwork = if (albumId > 0) {
                    ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                } else {
                    null
                }
                result.add(
                    Track(
                        id = "ms:$id",
                        uri = uri,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        dateAddedMs = cursor.getLong(addedCol) * 1000L,
                        artworkUri = artwork,
                        embeddedArt = null,
                        isOwnDownload = OutputPublisher.PUBLIC_FOLDER in path,
                        playable = isSupported(uri, mime),
                        folder = LibraryGroups.folderName(
                            path,
                            legacyFilePath = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
                        ),
                    ),
                )
                if (result.size >= MAX_TRACKS) break
            }
        }
        return result.map { track ->
            if (!track.isOwnDownload) {
                track
            } else {
                val art = runCatching {
                    readSafMetadata(context, Uri.parse(track.uri)).art
                }.getOrNull()
                if (art != null) track.copy(embeddedArt = art) else track
            }
        }
    }

    private fun queryChosenFolder(context: Context, knownNames: MutableSet<String>): List<Track> {
        val treeUriString = PrefsRepository.folderUriValue() ?: return emptyList()
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return emptyList()
        if (!tree.exists() || !tree.canRead()) return emptyList()
        val result = mutableListOf<Track>()
        val candidates = mutableListOf<DocumentFile>()
        collectSafFiles(tree, 0, candidates)
        for (doc in candidates) {
            if (result.size >= MAX_TRACKS) break
            if (!doc.isFile) continue
            val name = doc.name ?: continue
            if (!isSupported(name, null)) continue
            val uri = doc.uri
            val meta = runCatching { readSafMetadata(context, uri) }.getOrNull() ?: continue
            val title = meta.title ?: name.substringBeforeLast('.')
            val key = trackKey(name.substringBeforeLast('.'), meta.durationMs)
            if (!knownNames.add(key)) continue
            result.add(
                Track(
                    id = "saf:$uri",
                    uri = uri.toString(),
                    title = title,
                    artist = meta.artist,
                    album = meta.album,
                    durationMs = meta.durationMs,
                    dateAddedMs = doc.lastModified(),
                    artworkUri = null,
                    embeddedArt = meta.art,
                    isOwnDownload = true,
                    playable = true,
                    folder = doc.parentFile?.name ?: tree.name,
                ),
            )
        }
        return result
    }

    private fun collectSafFiles(dir: DocumentFile, depth: Int, out: MutableList<DocumentFile>) {
        if (depth > 3 || out.size >= MAX_TRACKS * 2) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        children.forEach { child ->
            if (child.isDirectory) {
                collectSafFiles(child, depth + 1, out)
            } else if (child.isFile) {
                out.add(child)
            }
        }
    }

    private data class SafMeta(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val art: ByteArray?,
    )

    private fun readSafMetadata(context: Context, uri: Uri): SafMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            extractMeta(retriever)
        } catch (_: Exception) {
            SafMeta(null, null, null, 0L, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readFileMetadata(path: String): SafMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            extractMeta(retriever)
        } catch (_: Exception) {
            SafMeta(null, null, null, 0L, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractMeta(retriever: MediaMetadataRetriever): SafMeta {
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
        val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            ?.takeIf { it.isNotBlank() }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val art = runCatching { retriever.embeddedPicture }
            .getOrNull()?.takeIf { it.size > 512 }
        return SafMeta(title, rawArtist, album, durationMs, art)
    }

    fun isSupported(uriOrName: String, mimeType: String?): Boolean {
        val mime = mimeType?.lowercase().orEmpty()
        if (mime.startsWith("audio/")) return true
        val ext = uriOrName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_EXTENSIONS
    }

    private val SUPPORTED_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "ogg", "oga", "opus", "flac", "wav", "amr", "mid", "midi",
    )

    private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    private const val MAX_TRACKS = 1000
}
