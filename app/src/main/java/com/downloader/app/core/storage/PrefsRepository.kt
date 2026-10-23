package com.downloader.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.downloader.app.core.music.MusicPlaylist
import com.downloader.app.core.music.PlaylistStore
import com.downloader.app.core.util.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object PrefsRepository {

    private const val FILE = "downloader_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_FOLDER_URI = "download_tree_uri"
    private const val KEY_TURBO = "turbo_download"
    private const val KEY_EQ_ENABLED = "eq_enabled"
    private const val KEY_EQ_PRESET = "eq_preset"
    private const val KEY_EQ_GAINS = "eq_gains"
    private const val KEY_FAVORITE_TRACKS = "favorite_tracks"
    private const val KEY_MUSIC_PLAYLISTS = "music_playlists"
    private const val KEY_THEME_MODE = "appearance_theme_mode"
    private const val KEY_ACCENT_PALETTE = "appearance_accent_palette"
    private const val KEY_ACCENT_CUSTOM_HEX = "appearance_accent_custom_hex"
    private const val KEY_FONT_CHOICE = "appearance_font_choice"
    private const val KEY_CUSTOM_FONT_PATH = "appearance_custom_font_path"

    private var prefs: SharedPreferences? = null

    private val _language = MutableStateFlow(LocaleHelper.LANG_INDONESIAN)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _folderUri = MutableStateFlow<String?>(null)
    val folderUri: StateFlow<String?> = _folderUri.asStateFlow()

    private val _turbo = MutableStateFlow(false)
    val turbo: StateFlow<Boolean> = _turbo.asStateFlow()

    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqPreset = MutableStateFlow("Normal")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    private val _eqGains = MutableStateFlow("")
    val eqGains: StateFlow<String> = _eqGains.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTracks: StateFlow<Set<String>> = _favoriteTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<MusicPlaylist>>(emptyList())
    val playlists: StateFlow<List<MusicPlaylist>> = _playlists.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentPalette = MutableStateFlow(AccentPalette.BLUE)
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    private val _customAccentHex = MutableStateFlow("#3B82F6")
    val customAccentHex: StateFlow<String> = _customAccentHex.asStateFlow()

    private val _fontChoice = MutableStateFlow(FontChoice.DEFAULT)
    val fontChoice: StateFlow<FontChoice> = _fontChoice.asStateFlow()

    private val _customFontPath = MutableStateFlow<String?>(null)
    val customFontPath: StateFlow<String?> = _customFontPath.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = store
        _language.value = LocaleHelper.normalize(store.getString(KEY_LANGUAGE, null))
        _folderUri.value = store.getString(KEY_FOLDER_URI, null)?.takeIf { it.isNotBlank() }
        _turbo.value = store.getBoolean(KEY_TURBO, false)
        _eqEnabled.value = store.getBoolean(KEY_EQ_ENABLED, true)
        _eqPreset.value = store.getString(KEY_EQ_PRESET, "Normal")?.takeIf { it.isNotBlank() } ?: "Normal"
        _eqGains.value = store.getString(KEY_EQ_GAINS, null).orEmpty()
        _favoriteTracks.value = store.getStringSet(KEY_FAVORITE_TRACKS, emptySet())?.toSet() ?: emptySet()
        _playlists.value = PlaylistStore.decode(store.getString(KEY_MUSIC_PLAYLISTS, null))
        _themeMode.value = ThemeMode.fromKey(store.getString(KEY_THEME_MODE, null))
        _accentPalette.value = AccentPalette.fromKey(store.getString(KEY_ACCENT_PALETTE, null))
        _customAccentHex.value = normalizeHexColor(
            store.getString(KEY_ACCENT_CUSTOM_HEX, null).orEmpty(),
        ) ?: "#3B82F6"
        _fontChoice.value = FontChoice.fromKey(store.getString(KEY_FONT_CHOICE, null))
        _customFontPath.value = store.getString(KEY_CUSTOM_FONT_PATH, null)?.takeIf { it.isNotBlank() }
        if (_fontChoice.value == FontChoice.CUSTOM) {
            val customFile = _customFontPath.value?.let(::File)
            if (customFile == null || !CustomFontManager.hasValidFontSignature(customFile)) {
                _fontChoice.value = FontChoice.DEFAULT
                store.edit().putString(KEY_FONT_CHOICE, FontChoice.DEFAULT.key).apply()
            }
        }
    }

    @Synchronized
    fun setEqEnabled(value: Boolean) {
        _eqEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_EQ_ENABLED, value)?.apply()
    }

    fun eqEnabledValue(): Boolean = _eqEnabled.value

    @Synchronized
    fun setEqPreset(value: String) {
        _eqPreset.value = value
        prefs?.edit()?.putString(KEY_EQ_PRESET, value)?.apply()
    }

    fun eqPresetValue(): String = _eqPreset.value

    @Synchronized
    fun setEqGainsCsv(value: String) {
        _eqGains.value = value
        prefs?.edit()?.putString(KEY_EQ_GAINS, value)?.apply()
    }

    fun eqGainsValue(): String = _eqGains.value

    @Synchronized
    fun setTurbo(value: Boolean) {
        _turbo.value = value
        prefs?.edit()?.putBoolean(KEY_TURBO, value)?.apply()
    }

    fun turboValue(): Boolean = _turbo.value

    @Synchronized
    fun setLanguage(value: String) {
        val normalized = LocaleHelper.normalize(value)
        _language.value = normalized
        prefs?.edit()?.putString(KEY_LANGUAGE, normalized)?.apply()
    }

    @Synchronized
    fun setFolderUri(context: Context, uri: String?) {
        if (prefs == null) init(context)
        _folderUri.value = uri?.takeIf { it.isNotBlank() }
        prefs?.edit()?.putString(KEY_FOLDER_URI, _folderUri.value)?.apply()
    }

    fun folderUriValue(): String? = _folderUri.value

    @Synchronized
    fun setThemeMode(value: ThemeMode) {
        _themeMode.value = value
        prefs?.edit()?.putString(KEY_THEME_MODE, value.key)?.apply()
    }

    @Synchronized
    fun setAccentPalette(value: AccentPalette) {
        _accentPalette.value = value
        prefs?.edit()?.putString(KEY_ACCENT_PALETTE, value.key)?.apply()
    }

    @Synchronized
    fun setCustomAccentHex(value: String) {
        val normalized = normalizeHexColor(value) ?: return
        _customAccentHex.value = normalized
        prefs?.edit()?.putString(KEY_ACCENT_CUSTOM_HEX, normalized)?.apply()
    }

    @Synchronized
    fun setFontChoice(value: FontChoice) {
        _fontChoice.value = value
        prefs?.edit()?.putString(KEY_FONT_CHOICE, value.key)?.apply()
    }

    @Synchronized
    fun setCustomFontPath(value: String?) {
        val clean = value?.takeIf { it.isNotBlank() }
        _customFontPath.value = clean
        prefs?.edit()?.putString(KEY_CUSTOM_FONT_PATH, clean)?.apply()
    }

    @Synchronized
    fun toggleFavoriteTrack(id: String): Boolean {
        val current = _favoriteTracks.value
        val updated = if (id in current) current - id else current + id
        _favoriteTracks.value = updated
        prefs?.edit()?.putStringSet(KEY_FAVORITE_TRACKS, updated)?.apply()
        return id in updated
    }

    fun isFavoriteTrack(id: String): Boolean = id in _favoriteTracks.value

    @Synchronized
    fun createPlaylist(name: String): String? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val now = System.currentTimeMillis()
        val id = PlaylistStore.newId(_playlists.value, now)
        storePlaylists(PlaylistStore.create(_playlists.value, clean, id, now))
        return id
    }

    @Synchronized
    fun renamePlaylist(id: String, name: String) {
        if (name.trim().isEmpty()) return
        storePlaylists(PlaylistStore.rename(_playlists.value, id, name))
    }

    @Synchronized
    fun deletePlaylist(id: String) {
        storePlaylists(PlaylistStore.delete(_playlists.value, id))
    }

    /** true bila lagu benar-benar ditambahkan (bukan duplikat). */
    @Synchronized
    fun addTrackToPlaylist(playlistId: String, trackId: String): Boolean {
        val before = _playlists.value
        val updated = PlaylistStore.addTrack(before, playlistId, trackId)
        if (updated == before) return false
        storePlaylists(updated)
        return true
    }

    @Synchronized
    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        storePlaylists(PlaylistStore.removeTrack(_playlists.value, playlistId, trackId))
    }

    fun playlistValue(id: String): MusicPlaylist? = _playlists.value.firstOrNull { it.id == id }

    private fun storePlaylists(updated: List<MusicPlaylist>) {
        _playlists.value = updated
        prefs?.edit()?.putString(KEY_MUSIC_PLAYLISTS, PlaylistStore.encode(updated))?.apply()
    }
}
