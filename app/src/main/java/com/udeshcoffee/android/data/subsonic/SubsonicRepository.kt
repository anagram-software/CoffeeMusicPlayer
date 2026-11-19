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
import okhttp3.Request
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

data class ArtistInfo(val biography: String?, val imageUrl: String?, val similarArtists: List<Artist> = emptyList())

/** A server album page's data: its tracks plus details for the header. */
data class ServerAlbumDetails(val tracks: AlbumTracks, val genre: String?, val year: Int)

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

    private val _starredAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    /** Starred server album IDs (the heart on album pages). */
    val starredAlbumIds: StateFlow<Set<String>> = _starredAlbumIds.asStateFlow()

    private val _starredArtistIds = MutableStateFlow<Set<String>>(emptySet())
    /** Starred server artist IDs (the heart on artist pages). */
    val starredArtistIds: StateFlow<Set<String>> = _starredArtistIds.asStateFlow()

    private fun clearStars() {
        _starredIds.value = emptySet()
        _starredAlbumIds.value = emptySet()
        _starredArtistIds.value = emptySet()
    }

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
        clearStars()
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
        clearStars()
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

    // Behind Octo-Fiesta, getAlbum, getArtist and getTopSongs also return streaming-provider items
    // (see External.kt). The functions below keep the library's apart and play only those.

    /** An album's details and tracks (behind Octo-Fiesta, also the provider tracks the library copy lacks). */
    suspend fun albumDetails(albumId: String): ServerAlbumDetails {
        val album = client().get("getAlbum", "album", SubAlbum.serializer(), listOf("id" to albumId))
        if (album.starred != null) _starredAlbumIds.update { it + albumId }
        return ServerAlbumDetails(splitAlbumTracks(album.song.toSongs()), album.genre?.takeIf { it.isNotBlank() }, album.year)
    }

    /** An album's library tracks and, behind Octo-Fiesta, the provider tracks the library copy lacks. */
    suspend fun album(albumId: String): AlbumTracks = albumDetails(albumId).tracks

    /** What playing the album plays: its library tracks (all tracks for a provider album). */
    suspend fun albumSongs(albumId: String): List<Song> = album(albumId).playable

    private val _addedAlbums = MutableStateFlow<Set<String>>(emptySet())
    /** Albums asked to be added to the library this session (Octo-Fiesta reports no download progress). */
    val addedAlbums: StateFlow<Set<String>> = _addedAlbums.asStateFlow()

    /**
     * Has Octo-Fiesta download an album's [missing] provider tracks into the library. Preferably by
     * starring the provider album, which Octo-Fiesta turns into a background download of the whole
     * album (kept permanently, without passing the star on to Navidrome). Without a provider album
     * ID, starts streaming each missing song and hangs up once it begins: the server finishes the
     * download anyway. As in the web player (`addToLibrary.ts`).
     */
    suspend fun addAlbumToLibrary(albumId: String, missing: List<Song>) {
        val externalAlbum = externalAlbumId(albumId, missing)
        if (externalAlbum != null) {
            client().call("star", listOf("albumId" to externalAlbum))
        } else {
            val client = client()
            withContext(Dispatchers.IO) {
                for (song in missing) {
                    val id = song.remoteId ?: continue
                    // A failed song shouldn't stop the rest.
                    runCatching { Http.client.newCall(Request.Builder().url(client.url("stream", listOf("id" to id))).build()).execute().close() }
                }
            }
        }
        _addedAlbums.update { it + albumId }
    }

    private suspend fun getArtist(artistId: String): SubArtist =
            client().get("getArtist", "artist", SubArtist.serializer(), listOf("id" to artistId))

    /** The artist's albums, library and provider ones (one request; [groupReleases] separates them). */
    suspend fun artistAlbums(artistId: String): List<Album> = getArtist(artistId).also {
        if (it.starred != null) _starredArtistIds.update { ids -> ids + artistId }
    }.album.map { it.toAlbum() }

    /**
     * What playing the artist plays: the songs of their library albums (a few requests at a time),
     * without provider albums or tracks. A provider artist (or one with nothing in the library)
     * plays their top songs instead.
     */
    suspend fun artistSongs(artistId: String): List<Song> = coroutineScope {
        val artist = getArtist(artistId)
        val library = artist.album.filter { !isExternalId(it.id) }
        if (library.isEmpty()) return@coroutineScope topSongs(artistId, artist.name, count = 50)
        val gate = Semaphore(4)
        library.map { album ->
            async { gate.withPermit { album(album.id).library } }
        }.awaitAll().flatten()
    }

    /**
     * The artist's most popular songs (Navidrome has them when Last.fm is set up). [artistId] is used
     * by servers with the topSongsByArtistId extension, others match [artistName].
     */
    suspend fun topSongs(artistId: String, artistName: String, count: Int = 10): List<Song> =
            client().get("getTopSongs", "topSongs", SubSongs.serializer(),
                    listOf("id" to artistId, "artist" to artistName, "count" to count)).song.toSongs()

    suspend fun artistInfo(artistId: String): ArtistInfo {
        val info = client().get("getArtistInfo2", "artistInfo2", SubArtistInfo.serializer(), listOf("id" to artistId, "count" to 12))
        return ArtistInfo(info.biography?.takeIf { it.isNotBlank() }, info.largeImageUrl ?: info.mediumImageUrl,
                info.similarArtist.map { it.toArtist() })
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

    /** Starred songs; also refreshes [starredIds], [starredAlbumIds] and [starredArtistIds]. */
    suspend fun loadStarred(): List<Song> {
        val starred = client().get("getStarred2", "starred2", SubSearchResult.serializer())
        _starredIds.value = starred.song.map { it.id }.toSet()
        _starredAlbumIds.value = starred.album.map { it.id }.toSet()
        _starredArtistIds.value = starred.artist.map { it.id }.toSet()
        return starred.song.toSongs()
    }

    /** Stars or unstars an album (optimistically updating [starredAlbumIds]). */
    suspend fun setAlbumStarred(albumId: String, starred: Boolean) = setCollectionStarred(_starredAlbumIds, "albumId", albumId, starred)

    /** Stars or unstars an artist (optimistically updating [starredArtistIds]). */
    suspend fun setArtistStarred(artistId: String, starred: Boolean) = setCollectionStarred(_starredArtistIds, "artistId", artistId, starred)

    private suspend fun setCollectionStarred(ids: MutableStateFlow<Set<String>>, param: String, id: String, starred: Boolean) {
        val before = ids.value
        ids.update { if (starred) it + id else it - id }
        try {
            client().call(if (starred) "star" else "unstar", listOf(param to id))
        } catch (e: SubsonicException) {
            ids.value = before
            throw e
        }
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
