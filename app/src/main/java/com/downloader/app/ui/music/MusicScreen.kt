package com.downloader.app.ui.music

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.music.LibraryGroups
import com.downloader.app.core.music.MusicController
import com.downloader.app.core.music.MusicLibrary
import com.downloader.app.core.music.MusicPlaylist
import com.downloader.app.core.music.MusicSort
import com.downloader.app.core.music.Track
import com.downloader.app.core.music.TrackQuery
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderText
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.components.ArtworkTile
import com.downloader.app.ui.components.artworkInitials
import com.downloader.app.ui.components.DownloaderChip
import com.downloader.app.ui.components.DownloaderSearchField
import com.downloader.app.ui.components.MotionDialog
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import kotlinx.coroutines.delay

/**
 * Music Library: menelusuri, mencari, dan mengurutkan musik lokal.
 *
 * Kontrol pemutaran tidak lagi menempel di sini — ada di layar Now Playing dan mini player,
 * supaya daftar lagu tetap lega.
 */
@Composable
fun MusicScreen(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel(),
) {
    val context = LocalContext.current
    val tracks by MusicLibrary.tracks.collectAsState()
    val scanState by MusicLibrary.scanState.collectAsState()
    val granted by MusicLibrary.permissionGranted.collectAsState()
    val current by MusicController.currentTrack.collectAsState()
    val playing by MusicController.isPlaying.collectAsState()
    val playbackError by MusicController.error.collectAsState()
    val favoriteIds by PrefsRepository.favoriteTracks.collectAsState()
    val playlists by PrefsRepository.playlists.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onPermissionResult(it) }

    LaunchedEffect(Unit) {
        viewModel.updatePermission()
        if (MusicLibrary.tracks.value.isEmpty()) {
            viewModel.refresh()
        }
    }

    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var addTargetSnapshot by remember { mutableStateOf<Track?>(null) }
    var createPlaylistDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<MusicPlaylist?>(null) }
    var renameSnapshot by remember { mutableStateOf<MusicPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<MusicPlaylist?>(null) }
    var deleteSnapshot by remember { mutableStateOf<MusicPlaylist?>(null) }
    var notice by remember { mutableStateOf<Int?>(null) }
    var noticeArg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(addToPlaylistTrack) {
        if (addToPlaylistTrack != null) addTargetSnapshot = addToPlaylistTrack
    }
    LaunchedEffect(renameTarget) {
        if (renameTarget != null) renameSnapshot = renameTarget
    }
    LaunchedEffect(deleteTarget) {
        if (deleteTarget != null) deleteSnapshot = deleteTarget
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2600)
            notice = null
            noticeArg = null
        }
    }

    val unknownArtist = DownloaderApp.str(R.string.player_unknown_artist)
    val unknownAlbum = DownloaderApp.str(R.string.player_unknown_album)
    val unknownFolder = DownloaderApp.str(R.string.player_unknown_folder)

    val matched = remember(tracks, viewModel.query, viewModel.sort) {
        TrackQuery.filterSort(tracks, viewModel.query, viewModel.sort)
    }
    val favorites = remember(matched, favoriteIds) { matched.filter { it.id in favoriteIds } }
    val recentPreview = remember(tracks) { tracks.sortedByDescending { it.dateAddedMs }.take(4) }
    val albums = remember(matched, unknownAlbum) { LibraryGroups.albums(matched, unknownAlbum) }
    val artists = remember(matched, unknownArtist) { LibraryGroups.artists(matched, unknownArtist) }
    val folders = remember(matched, unknownFolder) { LibraryGroups.folders(matched, unknownFolder) }

    val detail = viewModel.detail

    // Tombol kembali menutup layar turunan (album/artis/folder/playlist) lebih dulu.
    BackHandler(enabled = detail != null) { viewModel.closeDetail() }

    val openPlayer: (Track, List<Track>) -> Unit = { track, list ->
        viewModel.playTrack(track, list)
        onOpenPlayer()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (detail == null) {
                item(key = "header") {
                    MusicHeader(
                        scanning = scanState == MusicLibrary.ScanState.Loading,
                        onRescan = viewModel::refresh,
                        modifier = Modifier.entrance(0, offsetY = 10),
                    )
                }

                if (!granted && tracks.isEmpty()) {
                    item(key = "permission") {
                        PermissionCard(
                            onGrant = { permissionLauncher.launch(MusicLibrary.audioPermission()) },
                            onOpenSettings = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null),
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }

                if (playbackError != null) {
                    item(key = "playback-error") {
                        PlaybackErrorBanner(
                            message = playbackError,
                            onDismiss = MusicController::dismissError,
                        )
                    }
                }

                item(key = "search") {
                    DownloaderSearchField(
                        value = viewModel.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = DownloaderApp.str(R.string.player_search_hint),
                        modifier = Modifier.entrance(1, offsetY = 10),
                    )
                }

                item(key = "categories") {
                    Column(
                        modifier = Modifier.entrance(2, offsetY = 10),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CategoryChip(LibraryCategory.SONGS, viewModel)
                            CategoryChip(LibraryCategory.ALBUMS, viewModel)
                            CategoryChip(LibraryCategory.ARTISTS, viewModel)
                            CategoryChip(LibraryCategory.FOLDERS, viewModel)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CategoryChip(LibraryCategory.FAVORITES, viewModel)
                            CategoryChip(LibraryCategory.PLAYLISTS, viewModel)
                            CategoryChip(LibraryCategory.RECENT, viewModel)
                        }
                    }
                }
            }

            val libraryBusy = scanState == MusicLibrary.ScanState.Loading && tracks.isEmpty()
            val libraryFailed = scanState is MusicLibrary.ScanState.Failed && tracks.isEmpty()

            when {
                detail != null -> {
                    val playlist = (detail as? LibraryDetail.Playlist)
                        ?.let { ref -> playlists.firstOrNull { it.id == ref.id } }
                    val groupTracks: List<Track> = when (detail) {
                        is LibraryDetail.Album -> albums.firstOrNull { it.key == detail.key }
                            ?.let { TrackQuery.filterSort(it.tracks, "", viewModel.sort) }
                            .orEmpty()

                        is LibraryDetail.Artist -> artists.firstOrNull { it.key == detail.key }
                            ?.let { TrackQuery.filterSort(it.tracks, "", viewModel.sort) }
                            .orEmpty()

                        is LibraryDetail.Folder -> folders.firstOrNull { it.key == detail.key }
                            ?.let { TrackQuery.filterSort(it.tracks, "", viewModel.sort) }
                            .orEmpty()

                        is LibraryDetail.Playlist -> playlist
                            ?.let { LibraryGroups.resolveTracks(it.trackIds, tracks) }
                            .orEmpty()
                    }
                    val detailTitle = when (detail) {
                        is LibraryDetail.Album -> albums.firstOrNull { it.key == detail.key }?.title
                        is LibraryDetail.Artist -> artists.firstOrNull { it.key == detail.key }?.name
                        is LibraryDetail.Folder -> folders.firstOrNull { it.key == detail.key }?.name
                        is LibraryDetail.Playlist -> playlist?.name
                    } ?: DownloaderApp.str(R.string.player_cat_songs)

                    item(key = "detail-header") {
                        Row(
                            modifier = Modifier.entrance(0, offsetY = 8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = viewModel::closeDetail) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = DownloaderApp.str(R.string.action_back),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detailTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = DownloaderApp.str(R.string.player_tracks_count, groupTracks.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (detail is LibraryDetail.Playlist && playlist != null) {
                                IconButton(onClick = { renameTarget = playlist }) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = DownloaderApp.str(R.string.player_playlist_rename),
                                    )
                                }
                                IconButton(onClick = { deleteTarget = playlist }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = DownloaderApp.str(R.string.player_playlist_delete),
                                    )
                                }
                            }
                        }
                    }

                    item(key = "detail-actions") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.playAll(groupTracks) }) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(DownloaderApp.str(R.string.player_play_all))
                            }
                            TextButton(onClick = { viewModel.shuffleAll(groupTracks) }) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(DownloaderApp.str(R.string.player_shuffle_all))
                            }
                        }
                    }

                    val missing = if (playlist != null) {
                        LibraryGroups.missingCount(playlist.trackIds, tracks)
                    } else {
                        0
                    }
                    if (missing > 0) {
                        item(key = "missing-note") {
                            Text(
                                text = DownloaderApp.str(R.string.player_playlist_missing, missing),
                                style = MaterialTheme.typography.labelSmall,
                                color = DownloaderColors.Warning,
                            )
                        }
                    }

                    if (groupTracks.isEmpty()) {
                        item(key = "detail-empty") {
                            EmptyState(
                                title = DownloaderApp.str(
                                    if (playlist != null) {
                                        R.string.player_playlist_empty_title
                                    } else {
                                        R.string.player_no_result_title
                                    },
                                ),
                                desc = DownloaderApp.str(
                                    if (playlist != null) {
                                        R.string.player_playlist_empty_desc
                                    } else {
                                        R.string.player_no_result_desc
                                    },
                                ),
                            )
                        }
                    } else {
                        itemsIndexed(groupTracks, key = { _, track -> track.id }) { index, track ->
                            LibraryTrackRow(
                                track = track,
                                isCurrent = track.id == current?.id,
                                isPlaying = track.id == current?.id && playing,
                                isFavorite = track.id in favoriteIds,
                                onClick = { openPlayer(track, groupTracks) },
                                onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                onPlayNext = { viewModel.playNext(track) },
                                onAddToQueue = {
                                    viewModel.addToQueue(track)
                                    notice = R.string.player_queued
                                },
                                onAddToPlaylist = { addToPlaylistTrack = track },
                                onRemove = playlist?.let { pl ->
                                    {
                                        viewModel.removeFromPlaylist(pl.id, track.id)
                                        notice = R.string.player_playlist_removed
                                    }
                                },
                                modifier = Modifier
                                    .entrance(index.coerceAtMost(8), offsetY = 10)
                                    .animateItem(),
                            )
                        }
                    }
                }

                libraryBusy -> item(key = "loading") { LibraryLoading() }

                libraryFailed -> item(key = "scan-error") {
                    LibraryScanError(onRetry = viewModel::refresh)
                }

                tracks.isEmpty() && viewModel.category != LibraryCategory.SONGS ->
                    item(key = "empty") { LibraryEmpty() }

                viewModel.category == LibraryCategory.PLAYLISTS -> {
                    item(key = "playlists-header") {
                        SectionHeader(
                            title = DownloaderApp.str(R.string.player_cat_playlists),
                            count = DownloaderApp.str(R.string.player_playlists_count, playlists.size),
                        )
                    }
                    item(key = "new-playlist") {
                        NewPlaylistRow(onClick = { createPlaylistDialog = true })
                    }
                    if (playlists.isEmpty()) {
                        item(key = "no-playlists") {
                            EmptyState(
                                title = DownloaderApp.str(R.string.player_playlists_empty_title),
                                desc = DownloaderApp.str(R.string.player_playlists_empty_desc),
                            )
                        }
                    } else {
                        items(playlists, key = { "pl-${it.id}" }) { item ->
                            val members = LibraryGroups.resolveTracks(item.trackIds, tracks)
                            GroupRow(
                                title = item.name,
                                subtitle = DownloaderApp.str(R.string.player_tracks_count, members.size),
                                sample = members.firstOrNull(),
                                fallbackIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                                onClick = {
                                    viewModel.openDetail(LibraryDetail.Playlist(item.id))
                                },
                            )
                        }
                    }
                }

                viewModel.category == LibraryCategory.ALBUMS -> {
                    item(key = "albums-header") {
                        SectionHeader(
                            title = DownloaderApp.str(R.string.player_cat_albums),
                            count = DownloaderApp.str(R.string.player_albums_count, albums.size),
                        )
                    }
                    if (albums.isEmpty()) {
                        item(key = "no-albums") { LibraryNoResult() }
                    } else {
                        items(albums, key = { "al-${it.key}" }) { album ->
                            GroupRow(
                                title = album.title,
                                subtitle = album.artist
                                    ?: DownloaderApp.str(R.string.player_tracks_count, album.tracks.size),
                                sample = album.sample,
                                onClick = { viewModel.openDetail(LibraryDetail.Album(album.key)) },
                            )
                        }
                    }
                }

                viewModel.category == LibraryCategory.ARTISTS -> {
                    item(key = "artists-header") {
                        SectionHeader(
                            title = DownloaderApp.str(R.string.player_cat_artists),
                            count = DownloaderApp.str(R.string.player_artists_count, artists.size),
                        )
                    }
                    if (artists.isEmpty()) {
                        item(key = "no-artists") { LibraryNoResult() }
                    } else {
                        items(artists, key = { "ar-${it.key}" }) { artist ->
                            GroupRow(
                                title = artist.name,
                                subtitle = DownloaderApp.str(R.string.player_tracks_count, artist.tracks.size),
                                sample = artist.sample,
                                onClick = { viewModel.openDetail(LibraryDetail.Artist(artist.key)) },
                            )
                        }
                    }
                }

                viewModel.category == LibraryCategory.FOLDERS -> {
                    item(key = "folders-header") {
                        SectionHeader(
                            title = DownloaderApp.str(R.string.player_cat_folders),
                            count = DownloaderApp.str(R.string.player_folders_count, folders.size),
                        )
                    }
                    if (folders.isEmpty()) {
                        item(key = "no-folders") { LibraryNoResult() }
                    } else {
                        items(folders, key = { "fo-${it.key}" }) { folder ->
                            GroupRow(
                                title = folder.name,
                                subtitle = DownloaderApp.str(R.string.player_tracks_count, folder.tracks.size),
                                sample = folder.tracks.firstOrNull { it.artworkUri != null || it.embeddedArt != null },
                                fallbackIcon = Icons.Rounded.Folder,
                                onClick = { viewModel.openDetail(LibraryDetail.Folder(folder.key)) },
                            )
                        }
                    }
                }

                else -> {
                    val favoriteCategory = viewModel.category == LibraryCategory.FAVORITES
                    val recentCategory = viewModel.category == LibraryCategory.RECENT
                    val dashboard = !favoriteCategory && !recentCategory && viewModel.query.isBlank()
                    val list = when {
                        favoriteCategory -> favorites
                        recentCategory -> TrackQuery.filterSort(tracks, viewModel.query, MusicSort.RECENT)
                        else -> matched
                    }
                    if (dashboard) {
                        if (recentPreview.isNotEmpty()) {
                            item(key = "recent-header") {
                            SectionHeader(
                                title = DownloaderApp.str(R.string.player_cat_recent),
                                count = "",
                                actionLabel = DownloaderApp.str(R.string.history_see_all),
                                onAction = { viewModel.onCategoryChange(LibraryCategory.RECENT) },
                                modifier = Modifier.entrance(3, offsetY = 8),
                            )
                        }
                        items(recentPreview, key = { "recent-${it.id}" }) { track ->
                            LibraryTrackRow(
                                track = track,
                                isCurrent = track.id == current?.id,
                                isPlaying = track.id == current?.id && playing,
                                isFavorite = track.id in favoriteIds,
                                onClick = { openPlayer(track, recentPreview) },
                                onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                onPlayNext = { viewModel.playNext(track) },
                                onAddToQueue = {
                                    viewModel.addToQueue(track)
                                    notice = R.string.player_queued
                                },
                                onAddToPlaylist = { addToPlaylistTrack = track },
                                onRemove = null,
                                modifier = Modifier.animateItem(),
                            )
                        }
                        }

                        item(key = "quick-collections-header") {
                            SectionHeader(
                                title = DownloaderApp.str(R.string.player_quick_collections),
                                count = "",
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .entrance(4, offsetY = 8),
                            )
                        }
                        item(key = "quick-collections") {
                            QuickCollections(
                                favoriteCount = favorites.size,
                                playlistCount = playlists.size,
                                onFavorites = { viewModel.onCategoryChange(LibraryCategory.FAVORITES) },
                                onPlaylists = { viewModel.onCategoryChange(LibraryCategory.PLAYLISTS) },
                                modifier = Modifier.entrance(5, offsetY = 12),
                            )
                        }
                    }

                    item(key = "songs-header") {
                        SectionHeader(
                            title = DownloaderApp.str(
                                when {
                                    favoriteCategory -> R.string.player_cat_favorites
                                    recentCategory -> R.string.player_cat_recent
                                    else -> R.string.player_cat_songs
                                },
                            ),
                            count = DownloaderApp.str(R.string.player_tracks_count, list.size),
                            onPlayAll = { viewModel.playAll(list) },
                            onShuffleAll = { viewModel.shuffleAll(list) },
                            modifier = if (dashboard) {
                                Modifier
                                    .padding(top = 4.dp)
                                    .entrance(6, offsetY = 8)
                            } else {
                                Modifier
                            },
                        )
                    }

                    item(key = "sort") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SortChip(
                                selected = viewModel.sort == MusicSort.RECENT,
                                label = DownloaderApp.str(R.string.player_sort_recent),
                                onClick = { viewModel.onSortChange(MusicSort.RECENT) },
                            )
                            SortChip(
                                selected = viewModel.sort == MusicSort.NAME,
                                label = DownloaderApp.str(R.string.player_sort_name),
                                onClick = { viewModel.onSortChange(MusicSort.NAME) },
                            )
                            SortChip(
                                selected = viewModel.sort == MusicSort.ARTIST,
                                label = DownloaderApp.str(R.string.player_sort_artist),
                                onClick = { viewModel.onSortChange(MusicSort.ARTIST) },
                            )
                            SortChip(
                                selected = viewModel.sort == MusicSort.ALBUM,
                                label = DownloaderApp.str(R.string.player_sort_album),
                                onClick = { viewModel.onSortChange(MusicSort.ALBUM) },
                            )
                        }
                    }

                    when {
                        list.isEmpty() && favoriteCategory -> {
                            item(key = "no-favorites") {
                                EmptyState(
                                    title = DownloaderApp.str(R.string.player_no_favorites_title),
                                    desc = DownloaderApp.str(R.string.player_no_favorites_desc),
                                )
                            }
                        }

                        list.isEmpty() -> item(key = "no-result") {
                            if (viewModel.category == LibraryCategory.SONGS && viewModel.query.isBlank()) {
                                LibraryEmpty()
                            } else {
                                LibraryNoResult()
                            }
                        }

                        else -> {
                            itemsIndexed(list, key = { _, track -> track.id }) { index, track ->
                                LibraryTrackRow(
                                    track = track,
                                    isCurrent = track.id == current?.id,
                                    isPlaying = track.id == current?.id && playing,
                                    isFavorite = track.id in favoriteIds,
                                    onClick = { openPlayer(track, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                    onPlayNext = { viewModel.playNext(track) },
                                    onAddToQueue = {
                                        viewModel.addToQueue(track)
                                        notice = R.string.player_queued
                                    },
                                    onAddToPlaylist = { addToPlaylistTrack = track },
                                    onRemove = null,
                                    modifier = Modifier
                                        .entrance(index.coerceAtMost(8), offsetY = 10)
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom-space") { Spacer(Modifier.height(8.dp)) }
        }

        notice?.let { res ->
            NoticeBar(
                text = noticeArg?.let { arg -> DownloaderApp.str(res, arg) } ?: DownloaderApp.str(res),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }

    MotionDialog(
        visible = addToPlaylistTrack != null,
        onDismiss = { addToPlaylistTrack = null },
    ) {
        val selected = addTargetSnapshot
        if (selected != null) {
            AddToPlaylistContent(
                playlists = playlists,
                onDismiss = { addToPlaylistTrack = null },
                onPick = { target ->
                    val added = viewModel.addToPlaylist(target.id, selected.id)
                    notice = if (added) R.string.player_playlist_added else R.string.player_playlist_exists
                    noticeArg = target.name
                    addToPlaylistTrack = null
                },
                onCreateNew = {
                    addToPlaylistTrack = null
                    createPlaylistDialog = true
                },
            )
        }
    }

    MotionDialog(
        visible = createPlaylistDialog,
        onDismiss = { createPlaylistDialog = false },
    ) {
        PlaylistNameContent(
            title = DownloaderApp.str(R.string.player_new_playlist),
            initial = "",
            confirmLabel = DownloaderApp.str(R.string.player_playlist_create),
            onDismiss = { createPlaylistDialog = false },
            onConfirm = { name ->
                if (viewModel.createPlaylist(name) != null) {
                    notice = R.string.player_playlist_created
                    noticeArg = name
                }
                createPlaylistDialog = false
            },
        )
    }

    MotionDialog(
        visible = renameTarget != null,
        onDismiss = { renameTarget = null },
    ) {
        val target = renameSnapshot
        if (target != null) {
            androidx.compose.runtime.key(target.id) {
                PlaylistNameContent(
                    title = DownloaderApp.str(R.string.player_playlist_rename),
                    initial = target.name,
                    confirmLabel = DownloaderApp.str(R.string.action_change),
                    onDismiss = { renameTarget = null },
                    onConfirm = { name ->
                        viewModel.renamePlaylist(target.id, name)
                        renameTarget = null
                    },
                )
            }
        }
    }

    MotionDialog(
        visible = deleteTarget != null,
        onDismiss = { deleteTarget = null },
    ) {
        val target = deleteSnapshot
        if (target != null) {
            Text(
                text = DownloaderApp.str(R.string.player_playlist_delete),
                style = DownloaderText.dialogTitle,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = DownloaderApp.str(R.string.player_playlist_delete_confirm, target.name),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(DownloaderApp.str(R.string.action_cancel))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text(
                        text = DownloaderApp.str(R.string.action_delete),
                        color = DownloaderColors.Danger,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: LibraryCategory, viewModel: MusicViewModel) {
    val label = when (category) {
        LibraryCategory.SONGS -> R.string.player_cat_songs
        LibraryCategory.ALBUMS -> R.string.player_cat_albums
        LibraryCategory.ARTISTS -> R.string.player_cat_artists
        LibraryCategory.FOLDERS -> R.string.player_cat_folders
        LibraryCategory.FAVORITES -> R.string.player_cat_favorites
        LibraryCategory.PLAYLISTS -> R.string.player_cat_playlists
        LibraryCategory.RECENT -> R.string.player_cat_recent
    }
    DownloaderChip(
        label = DownloaderApp.str(label),
        selected = viewModel.category == category,
        onClick = { viewModel.onCategoryChange(category) },
    )
}

@Composable
private fun NoticeBar(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = DownloaderColors.SurfaceElevated,
        tonalElevation = 4.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: String,
    onPlayAll: (() -> Unit)? = null,
    onShuffleAll: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = DownloaderText.sectionTitle,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (count.isNotBlank()) {
                Text(
                    text = count,
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        style = DownloaderText.meta,
                        color = DownloaderColors.Primary,
                    )
                }
            }
        }
        if (onPlayAll != null && onShuffleAll != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPlayAll) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(DownloaderApp.str(R.string.player_play_all))
                }
                TextButton(onClick = onShuffleAll) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(DownloaderApp.str(R.string.player_shuffle_all))
                }
            }
        }
    }
}

@Composable
private fun QuickCollections(
    favoriteCount: Int,
    playlistCount: Int,
    onFavorites: () -> Unit,
    onPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        CollectionCard(
            title = DownloaderApp.str(R.string.player_cat_favorites),
            subtitle = DownloaderApp.str(R.string.player_collection_favorites_desc, favoriteCount),
            icon = Icons.Rounded.Favorite,
            accent = DownloaderColors.Favorite,
            onClick = onFavorites,
            modifier = Modifier.weight(1f),
        )
        CollectionCard(
            title = DownloaderApp.str(R.string.player_cat_playlists),
            subtitle = DownloaderApp.str(R.string.player_collection_playlists_desc, playlistCount),
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            accent = DownloaderColors.Cyan,
            onClick = onPlaylists,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CollectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = DownloaderColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier
            .height(132.dp)
            .pressScale(scaleDown = 0.96f),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(25.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = DownloaderText.rowTitle,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = DownloaderApp.str(R.string.player_collection_open),
                style = DownloaderText.metaSm,
                color = DownloaderColors.Primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NewPlaylistRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DownloaderRadius.md))
            .clickable(onClick = onClick)
            .pressScale(scaleDown = 0.985f)
            .padding(horizontal = 5.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(49.dp)
                .clip(RoundedCornerShape(DownloaderRadius.artwork))
                .background(DownloaderColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = DownloaderColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = DownloaderApp.str(R.string.player_new_playlist),
            style = DownloaderText.rowTitle,
            color = DownloaderColors.TextPrimary,
        )
    }
}

@Composable
private fun GroupRow(
    title: String,
    subtitle: String,
    sample: Track?,
    onClick: () -> Unit,
    fallbackIcon: ImageVector = Icons.Rounded.MusicNote,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DownloaderRadius.md))
            .clickable(onClick = onClick)
            .pressScale(scaleDown = 0.985f)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sample != null) {
            TrackArtwork(
                track = sample,
                modifier = Modifier.size(44.dp),
                corner = DownloaderRadius.artwork,
                iconSize = 22.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(DownloaderRadius.artwork))
                    .background(DownloaderColors.SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    fallbackIcon,
                    contentDescription = null,
                    tint = DownloaderColors.TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DownloaderText.rowTitle,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val currentBorder by animateColorAsState(
        targetValue = if (isCurrent) DownloaderColors.PrimarySoft else Color.Transparent,
        animationSpec = tween(240),
        label = "trackBorder",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) DownloaderColors.PrimaryLight else DownloaderColors.TextPrimary,
        animationSpec = tween(220),
        label = "trackTitle",
    )
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "favoriteScale",
    )

    val shape = RoundedCornerShape(DownloaderRadius.row)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, currentBorder, shape)
            .clickable(onClick = onClick)
            .pressScale(scaleDown = 0.985f)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            track = track,
            modifier = Modifier.size(44.dp),
            corner = DownloaderRadius.artwork,
            iconSize = 22.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = DownloaderText.rowTitleSm,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${track.artist ?: DownloaderApp.str(R.string.player_unknown_artist)} • " +
                    Formatters.durationLong(track.durationMs / 1000),
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (track.isOwnDownload) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = DownloaderApp.str(R.string.player_own_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        if (isCurrent) {
            Spacer(Modifier.width(6.dp))
            Icon(
                if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Pause,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (!track.playable) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Close,
                contentDescription = DownloaderApp.str(R.string.player_missing_file),
                tint = DownloaderColors.Warning,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = DownloaderApp.str(
                    if (isFavorite) R.string.player_favorite_remove else R.string.player_favorite_add,
                ),
                tint = if (isFavorite) DownloaderColors.Primary else DownloaderColors.TextFaint,
                modifier = Modifier
                    .size(19.dp)
                    .scale(favoriteScale),
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = DownloaderApp.str(R.string.player_more_actions),
                    tint = DownloaderColors.TextFaint,
                    modifier = Modifier.size(19.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(DownloaderApp.str(R.string.player_play_now)) },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    onClick = {
                        menu = false
                        onClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(DownloaderApp.str(R.string.player_play_next)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                    onClick = {
                        menu = false
                        onPlayNext()
                    },
                )
                DropdownMenuItem(
                    text = { Text(DownloaderApp.str(R.string.player_add_queue)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                    onClick = {
                        menu = false
                        onAddToQueue()
                    },
                )
                DropdownMenuItem(
                    text = { Text(DownloaderApp.str(R.string.player_add_playlist)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        menu = false
                        onAddToPlaylist()
                    },
                )
                if (onRemove != null) {
                    DropdownMenuItem(
                        text = { Text(DownloaderApp.str(R.string.player_remove_from_playlist)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menu = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistContent(
    playlists: List<MusicPlaylist>,
    onDismiss: () -> Unit,
    onPick: (MusicPlaylist) -> Unit,
    onCreateNew: () -> Unit,
) {
    Text(
        text = DownloaderApp.str(R.string.player_add_playlist),
        style = DownloaderText.dialogTitle,
        color = DownloaderColors.TextPrimary,
    )
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onCreateNew)
                .pressScale(scaleDown = 0.98f)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = DownloaderColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = DownloaderApp.str(R.string.player_new_playlist),
                style = DownloaderText.rowTitle,
                color = DownloaderColors.Primary,
            )
        }
        if (playlists.isEmpty()) {
            Text(
                text = DownloaderApp.str(R.string.player_playlists_empty_desc),
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            playlists.forEach { playlist ->
                Text(
                    text = playlist.name,
                    style = DownloaderText.rowTitle,
                    color = DownloaderColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPick(playlist) }
                        .pressScale(scaleDown = 0.98f)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text(DownloaderApp.str(R.string.action_cancel))
        }
    }
}

@Composable
private fun PlaylistNameContent(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    Text(
        text = title,
        style = DownloaderText.dialogTitle,
        color = DownloaderColors.TextPrimary,
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(DownloaderApp.str(R.string.player_playlist_name)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) {
            Text(DownloaderApp.str(R.string.action_cancel))
        }
        Spacer(Modifier.width(4.dp))
        TextButton(
            onClick = { onConfirm(value.trim()) },
            enabled = value.trim().isNotEmpty(),
        ) {
            Text(confirmLabel, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MusicHeader(
    scanning: Boolean,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DownloaderApp.str(R.string.player_title),
                    style = DownloaderText.screenTitle,
                    color = DownloaderColors.TextPrimary,
                )
                Spacer(Modifier.width(8.dp))
                BetaBadge()
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = DownloaderApp.str(R.string.player_desc),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
            )
        }
        if (scanning) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onRescan) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = DownloaderApp.str(R.string.player_rescan),
                )
            }
        }
    }
}

@Composable
fun BetaBadge(compact: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(if (compact) 4.dp else 5.dp),
        color = DownloaderColors.PrimarySoft,
        contentColor = DownloaderColors.TextOnSoft,
    ) {
        Text(
            // Desain memakai simbol β di samping label nav; versi penuh memakai kata BETA.
            text = if (compact) "β" else DownloaderApp.str(R.string.player_beta),
            style = DownloaderText.micro,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 5.dp,
                vertical = 1.dp,
            ),
        )
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DownloaderColors.SurfaceElevated),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = DownloaderApp.str(R.string.player_need_permission_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = DownloaderApp.str(R.string.player_need_permission_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onGrant) {
                    Text(DownloaderApp.str(R.string.player_grant_access))
                }
                TextButton(onClick = onOpenSettings) {
                    Text(DownloaderApp.str(R.string.player_open_settings))
                }
            }
        }
    }
}

@Composable
private fun PlaybackErrorBanner(message: String?, onDismiss: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message ?: DownloaderApp.str(R.string.player_error_generic),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = DownloaderApp.str(R.string.action_close),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun SortChip(selected: Boolean, label: String, onClick: () -> Unit) {
    DownloaderChip(
        label = label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun TrackArtwork(
    track: Track,
    modifier: Modifier = Modifier,
    corner: Dp,
    iconSize: Dp,
) {
    val model = track.embeddedArt ?: track.artworkUri
    Box(modifier = modifier) {
        val initials = artworkInitials(track.title)
        // Placeholder selalu berada di belakang gambar. Dengan begitu album
        // atau lagu yang metadata artwork-nya ada tetapi berkas gambarnya
        // hilang tidak menyisakan kotak kosong di daftar.
        ArtworkTile(
            seed = track.id,
            modifier = Modifier.matchParentSize(),
            corner = corner,
            label = initials.ifBlank { null },
            icon = if (initials.isBlank()) Icons.Rounded.MusicNote else null,
            iconSize = iconSize,
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(corner)),
            )
        }
    }
}

@Composable
private fun LibraryLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = DownloaderApp.str(R.string.player_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryEmpty() {
    EmptyState(
        title = DownloaderApp.str(R.string.player_empty_title),
        desc = DownloaderApp.str(R.string.player_empty_desc),
    )
}

@Composable
private fun LibraryNoResult() {
    EmptyState(
        title = DownloaderApp.str(R.string.player_no_result_title),
        desc = DownloaderApp.str(R.string.player_no_result_desc),
    )
}

@Composable
private fun LibraryScanError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DownloaderColors.SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = DownloaderColors.Warning,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = DownloaderApp.str(R.string.player_scan_failed_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = DownloaderApp.str(R.string.player_scan_failed_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(DownloaderApp.str(R.string.job_retry))
        }
    }
}

@Composable
private fun EmptyState(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DownloaderColors.SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = DownloaderColors.TextFaint,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
