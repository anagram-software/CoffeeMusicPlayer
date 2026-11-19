package com.udeshcoffee.android.data.subsonic

import android.content.Context
import androidx.core.content.edit
import com.udeshcoffee.android.data.remote.Http
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/** A signed-in music server. */
@Serializable
data class SubsonicAccount(
        val credentials: SubsonicCredentials,
        /** e.g. "navidrome"; "subsonic" for servers without OpenSubsonic. */
        val serverType: String,
        val serverVersion: String,
        val extensions: Set<String> = emptySet()
) {
    /** A short label for the UI: the server's host name. */
    val name: String get() = credentials.url.substringAfter("://").substringBefore('/')
    val isSecure: Boolean get() = credentials.url.startsWith("https://", ignoreCase = true)
}

data class ArtistInfo(val biography: String?, val imageUrl: String?)

data class ServerSearchResults(val songs: List<Song>, val albums: List<Album>, val artists: List<Artist>)

/** Album list orders offered by getAlbumList2. */
enum class ServerAlbumSort(val type: String, val label: String) {
    NAME("alphabeticalByName", "Name"),
    ARTIST("alphabeticalByArtist", "Artist"),
    NEWEST("newest", "Recently added"),
    RECENT("recent", "Recently played"),
    FREQUENT("frequent", "Most played"),
    YEAR("byYear", "Year"),
    RANDOM("random", "Random")
}

/**
 * The Subsonic / OpenSubsonic music server (e.g. Navidrome): sign-in, library, playlists,
 * favorites (stars), lyrics, streaming and scrobbling. Results use the app's models with
 * [Song.remoteId] etc. set. Calls throw [SubsonicException] on failure; they are main-safe.
 */
class SubsonicRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _account = MutableStateFlow(loadAccount())
    /** The signed-in server, or null. */
    val account: StateFlow<SubsonicAccount?> = _account.asStateFlow()

    private var cachedClient: SubsonicClient? = null

    /** Bumped after playlist edits so playlist screens reload. */
    val playlistsVersion = MutableStateFlow(0)

    private val _starredIds = MutableStateFlow<Set<String>>(emptySet())
    /** Starred (favorite) server song IDs, kept up to date by [loadStarred] and [setStarred]. */
    val starredIds: StateFlow<Set<String>> = _starredIds.asStateFlow()

    // ---- Account ----

    val isSignedIn: Boolean get() = _account.value != null

    private fun client(): SubsonicClient {
        val account = _account.value ?: throw SubsonicException(0, "Not signed in to a music server")
        cachedClient?.takeIf { it.credentials == account.credentials }?.let { return it }
        return SubsonicClient(account.credentials) { Http.client }.also { cachedClient = it }
    }

    /** Checks the credentials and remembers the server. Throws [SubsonicException] with a readable message. */
    suspend fun signIn(url: String, username: String, password: String): SubsonicAccount {
        val client = SubsonicClient(SubsonicAuth.credentials(url, username, password)) { Http.client }
        val ping = client.call("ping")
        val openSubsonic = ping["openSubsonic"]?.jsonPrimitive?.booleanOrNull == true
        val extensions = if (openSubsonic) runCatching {
            client.get("getOpenSubsonicExtensions", "openSubsonicExtensions", ListSerializer(SubExtension.serializer()))
                    .map { it.name }.toSet()
        }.getOrDefault(emptySet()) else emptySet()
        val account = SubsonicAccount(
                credentials = client.credentials,
                serverType = if (openSubsonic) ping["type"]?.jsonPrimitive?.content ?: "opensubsonic" else "subsonic",
                serverVersion = (if (openSubsonic) ping["serverVersion"] else ping["version"])?.jsonPrimitive?.content ?: "",
                extensions = extensions)
        prefs.edit { putString(KEY_ACCOUNT, SubsonicClient.json.encodeToString(SubsonicAccount.serializer(), account)) }
        cachedClient = client
        _account.value = account
        _starredIds.value = emptySet()
        starredLoadedFor = null
        playlistsVersion.value++
        return account
    }

    /** Forgets the server. Use MusicSourceSwitcher.signOut, which also leaves the server source. */
    fun signOut() {
        prefs.edit { remove(KEY_ACCOUNT) }
        cachedClient = null
        starredLoadedFor = null
        _account.value = null
        _starredIds.value = emptySet()
        playlistsVersion.value++
    }

    private fun loadAccount(): SubsonicAccount? = prefs.getString(KEY_ACCOUNT, null)?.let {
        runCatching { SubsonicClient.json.decodeFromString(SubsonicAccount.serializer(), it) }.getOrNull()
    }

    // ---- Streaming settings ----

    /** 0 = no limit (only applies when the server transcodes). */
    var maxBitRate: Int
        get() = prefs.getInt(KEY_MAX_BITRATE, 0)
        set(value) = prefs.edit { putInt(KEY_MAX_BITRATE, value) }

    /** "raw" (original file), "mp3" or "opus". */
    var streamFormat: String
        get() = prefs.getString(KEY_FORMAT, "raw") ?: "raw"
        set(value) = prefs.edit { putString(KEY_FORMAT, value) }

    /** Report plays to the server (play counts; Last.fm / ListenBrainz through the server). */
    var scrobbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCROBBLE, true)
        set(value) = prefs.edit { putBoolean(KEY_SCROBBLE, value) }

    /** The stream URL for a server song, or null when signed out. Carries the auth token. */
    fun streamUrl(remoteId: String): String? {
        if (!isSignedIn) return null
        val params = buildList<Pair<String, Any?>> {
            add("id" to remoteId)
            if (streamFormat != "raw") add("format" to streamFormat)
            if (maxBitRate > 0) add("maxBitRate" to maxBitRate)
        }
        return client().url("stream", params).toString()
    }

    fun coverArtUrl(coverArt: String?, size: Int? = null): String? {
        if (coverArt == null || !isSignedIn) return null
        return client().url("getCoverArt", listOf("id" to coverArt, "size" to size)).toString()
    }

    // ---- Library ----

    /** All songs, a page at a time. Subsonic has no song list call; Navidrome returns everything for an empty search. */
    suspend fun songs(offset: Int, count: Int = PAGE_SIZE): List<Song> =
            search3("", songCount = count, songOffset = offset).song.toSongs()

    suspend fun albums(sort: ServerAlbumSort, offset: Int, count: Int = PAGE_SIZE): List<Album> {
        val params = mutableListOf<Pair<String, Any?>>("type" to sort.type, "size" to count, "offset" to offset)
        // byYear needs a range; from > to lists newest first.
        if (sort == ServerAlbumSort.YEAR) params += listOf("fromYear" to 9999, "toYear" to 0)
        return client().get("getAlbumList2", "albumList2", SubAlbumList.serializer(), params).album.map { it.toAlbum() }
    }

    suspend fun artists(): List<Artist> =
            client().get("getArtists", "artists", SubArtists.serializer()).index.flatMap { it.artist }.map { it.toArtist() }

    suspend fun genres(): List<Genre> =
            client().get("getGenres", "genres", SubGenres.serializer()).genre
                    .filter { it.value.isNotBlank() && it.songCount > 0 }
                    .sortedBy { it.value.lowercase() }
                    .map { it.toGenre() }

    suspend fun albumSongs(albumId: String): List<Song> =
            client().get("getAlbum", "album", SubAlbum.serializer(), listOf("id" to albumId)).song.toSongs()

    suspend fun artistAlbums(artistId: String): List<Album> =
            client().get("getArtist", "artist", SubArtist.serializer(), listOf("id" to artistId)).album.map { it.toAlbum() }

    /** Every song by the artist: the songs of all their albums (a few requests at a time). */
    suspend fun artistSongs(artistId: String): List<Song> = coroutineScope {
        val gate = Semaphore(4)
        artistAlbums(artistId).map { album ->
            async { gate.withPermit { albumSongs(album.remoteId!!) } }
        }.awaitAll().flatten()
    }

    suspend fun artistInfo(artistId: String): ArtistInfo {
        val info = client().get("getArtistInfo2", "artistInfo2", SubArtistInfo.serializer(), listOf("id" to artistId, "count" to 0))
        return ArtistInfo(info.biography?.takeIf { it.isNotBlank() }, info.largeImageUrl ?: info.mediumImageUrl)
    }

    suspend fun genreSongs(genre: String, offset: Int = 0, count: Int = PAGE_SIZE): List<Song> =
            client().get("getSongsByGenre", "songsByGenre", SubSongs.serializer(),
                    listOf("genre" to genre, "count" to count, "offset" to offset)).song.toSongs()

    suspend fun genreAlbums(genre: String): List<Album> =
            client().get("getAlbumList2", "albumList2", SubAlbumList.serializer(),
                    listOf("type" to "byGenre", "genre" to genre, "size" to 500)).album.map { it.toAlbum() }

    suspend fun randomSongs(size: Int = 200): List<Song> =
            client().get("getRandomSongs", "randomSongs", SubSongs.serializer(), listOf("size" to size)).song.toSongs()

    suspend fun search(query: String): ServerSearchResults {
        val r = search3(query, songCount = 50, albumCount = 20, artistCount = 20)
        return ServerSearchResults(r.song.toSongs(), r.album.map { it.toAlbum() }, r.artist.map { it.toArtist() })
    }

    private suspend fun search3(query: String, songCount: Int = 0, songOffset: Int = 0, albumCount: Int = 0, artistCount: Int = 0) =
            client().get("search3", "searchResult3", SubSearchResult.serializer(), listOf(
                    "query" to query, "songCount" to songCount, "songOffset" to songOffset,
                    "albumCount" to albumCount, "artistCount" to artistCount))

    // ---- Favorites (stars) ----

    private var starredLoadedFor: SubsonicCredentials? = null

    /** Loads [starredIds] once per sign-in (failures are ignored; it's retried next time). */
    suspend fun ensureStarredLoaded() {
        val credentials = _account.value?.credentials ?: return
        if (starredLoadedFor == credentials) return
        runCatching { loadStarred() }.onSuccess { starredLoadedFor = credentials }
    }

    /** Starred songs; also refreshes [starredIds]. */
    suspend fun loadStarred(): List<Song> {
        val songs = client().get("getStarred2", "starred2", SubSearchResult.serializer()).song
        _starredIds.value = songs.map { it.id }.toSet()
        return songs.toSongs()
    }

    /** Stars or unstars a song (optimistically updating [starredIds]). */
    suspend fun setStarred(songIds: Collection<String>, starred: Boolean) {
        if (songIds.isEmpty()) return
        val before = _starredIds.value
        _starredIds.update { if (starred) it + songIds else it - songIds.toSet() }
        try {
            client().call(if (starred) "star" else "unstar", songIds.map { "id" to it })
        } catch (e: SubsonicException) {
            _starredIds.value = before
            throw e
        }
    }

    // ---- Playlists ----

    suspend fun playlists(): List<Playlist> =
            client().get("getPlaylists", "playlists", SubPlaylists.serializer()).playlist.map { it.toPlaylist() }

    suspend fun playlistSongs(playlistId: String): List<Song> =
            client().get("getPlaylist", "playlist", SubPlaylist.serializer(), listOf("id" to playlistId)).entry.toSongs()

    suspend fun createPlaylist(name: String, songIds: List<String>) {
        client().call("createPlaylist", listOf("name" to name) + songIds.map { "songId" to it })
        playlistsVersion.value++
    }

    suspend fun addToPlaylist(playlistId: String, songIds: List<String>) {
        if (songIds.isEmpty()) return
        client().call("updatePlaylist", listOf("playlistId" to playlistId) + songIds.map { "songIdToAdd" to it })
        playlistsVersion.value++
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int) {
        client().call("updatePlaylist", listOf("playlistId" to playlistId, "songIndexToRemove" to index))
        playlistsVersion.value++
    }

    /** Replaces the playlist's songs (Subsonic has no move call; createPlaylist with an ID overwrites). */
    suspend fun setPlaylistSongs(playlistId: String, songIds: List<String>) {
        client().call("createPlaylist", listOf("playlistId" to playlistId) + songIds.map { "songId" to it })
        playlistsVersion.value++
    }

    suspend fun renamePlaylist(playlistId: String, name: String) {
        client().call("updatePlaylist", listOf("playlistId" to playlistId, "name" to name))
        playlistsVersion.value++
    }

    suspend fun deletePlaylist(playlistId: String) {
        client().call("deletePlaylist", listOf("id" to playlistId))
        playlistsVersion.value++
    }

    // ---- Lyrics and plays ----

    /**
     * The server's lyrics for a song as plain text lines (OpenSubsonic songLyrics extension, else the
     * classic getLyrics by artist and title), or null when it has none.
     */
    suspend fun lyrics(song: Song): List<String>? {
        val account = _account.value ?: return null
        val remoteId = song.remoteId ?: return null
        val lines = if ("songLyrics" in account.extensions) {
            client().get("getLyricsBySongId", "lyricsList", SubLyricsList.serializer(), listOf("id" to remoteId))
                    .structuredLyrics.firstOrNull { it.line.isNotEmpty() }?.line?.map { it.value }
        } else {
            client().get("getLyrics", "lyrics", SubLyrics.serializer(), listOf("artist" to song.artistName, "title" to song.title))
                    .value?.lines()
        }
        return lines?.takeIf { l -> l.any { it.isNotBlank() } }
    }

    /** Reports a play: [submission] false for "now playing", true once it counts as played. Failures are ignored. */
    suspend fun scrobble(remoteId: String, submission: Boolean) {
        if (!isSignedIn || !scrobbleEnabled) return
        runCatching {
            client().call("scrobble", listOfNotNull("id" to remoteId, "submission" to submission,
                    ("time" to System.currentTimeMillis()).takeIf { submission }))
        }
    }

    /** Full details of songs by ID (used to restore a saved queue if needed). */
    suspend fun song(remoteId: String): Song? = withContext(Dispatchers.IO) {
        runCatching { client().get("getSong", "song", SubSong.serializer(), listOf("id" to remoteId)).toSong() }.getOrNull()
    }

    companion object {
        /** Its own preferences file, excluded from backups (it holds the auth token). */
        const val PREFS = "subsonic"
        const val PAGE_SIZE = 200
        private const val KEY_ACCOUNT = "account"
        private const val KEY_MAX_BITRATE = "max_bitrate"
        private const val KEY_FORMAT = "stream_format"
        private const val KEY_SCROBBLE = "scrobble"
    }
}
