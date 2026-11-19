package com.udeshcoffee.android.data

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.media.songsToArtists
import com.udeshcoffee.android.data.model.SongStat
import com.udeshcoffee.android.data.model.UserPlaylist
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.utils.DopeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The device music library (MediaStore) plus user playlists (Room).
 *
 * `observe…` functions return Flows that re-query whenever MediaStore (or the relevant Room
 * table) changes; `load…`/suspend functions are one-shot. Everything runs on [Dispatchers.IO].
 */
class MediaRepository(
        context: Context,
        private val localDataSource: LocalDataSource
) {

    private val contentResolver: ContentResolver = context.contentResolver

    private val songProjection = arrayOf(MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.TRACK)

    private val songQuery = Query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songProjection,
            "(" + MediaStore.Audio.Media.IS_MUSIC + "=1 OR " + MediaStore.Audio.Media.IS_PODCAST + "=1 )",
            null,
            MediaStore.Audio.Media.TITLE
    )

    private val genreQuery = Query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null,
            null,
            MediaStore.Audio.Genres.NAME
    )

    // ---- Query plumbing ----

    /** Emits once immediately and again (conflated) whenever content under [uri] changes. */
    private fun changes(uri: Uri): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(uri, true, observer)
        trySend(Unit)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun <T> queryList(query: Query, mapper: (Cursor) -> T): List<T> =
            contentResolver.query(query.uri, query.projection, query.selection, query.args, query.sort)?.use { c ->
                val items = ArrayList<T>(c.count)
                while (c.moveToNext()) items.add(mapper(c))
                items
            } ?: emptyList()

    private fun <T> observeQuery(query: Query, observeUri: Uri = query.uri, mapper: (Cursor) -> T): Flow<List<T>> =
            changes(observeUri).map { queryList(query, mapper) }.flowOn(Dispatchers.IO)

    private suspend fun <T> load(query: Query, mapper: (Cursor) -> T): List<T> =
            withContext(Dispatchers.IO) { queryList(query, mapper) }

    private fun songsWithIds(ids: Collection<Long>): Query = songQuery.copy(
            selection = songQuery.selection + " AND " + MediaStore.Audio.Media._ID + " IN (" + ids.joinToString(",") + ")")

    // ---- Songs ----

    suspend fun loadSong(uri: Uri): List<Song> = load(songQuery.copy(uri = uri)) { Song(it) }

    fun observeSongs(): Flow<List<Song>> = observeQuery(songQuery) { Song(it) }

    suspend fun loadSongs(): List<Song> = load(songQuery) { Song(it) }

    // ---- Albums ----

    fun observeAlbums(): Flow<List<Album>> = observeSongs().map { songsToAlbums(it) }

    fun observeAlbumSongs(albumId: Long): Flow<List<Song>> = observeSongs().map { songs -> songs.filter { it.albumId == albumId } }

    suspend fun loadAlbumSongs(albumId: Long): List<Song> = loadSongs().filter { it.albumId == albumId }

    suspend fun loadAlbums(): List<Album> = songsToAlbums(loadSongs())

    // ---- Artists ----

    fun observeArtists(): Flow<List<Artist>> = observeSongs().map { songsToArtists(it) }

    fun observeArtistSongs(artistId: Long): Flow<List<Song>> = observeSongs().map { songs -> songs.filter { it.artistId == artistId } }

    suspend fun loadArtistSongs(artistId: Long): List<Song> = loadSongs().filter { it.artistId == artistId }

    suspend fun loadArtists(): List<Artist> = songsToArtists(loadSongs())

    fun observeArtistAlbums(artistName: String): Flow<List<Album>> =
            observeAlbums().map { albums -> albums.filter { it.artist == artistName } }

    // ---- Genres ----

    fun observeGenres(): Flow<List<Genre>> =
            changes(genreQuery.uri).map {
                // MediaStore keeps genres whose songs were retagged or deleted; hide those.
                queryList(genreQuery) { Genre(it) }.onEach { it.setArtsAndSongCount(contentResolver) }.filter { (it.no ?: 0) > 0 }
            }.flowOn(Dispatchers.IO)

    private fun genreSongsQuery(genreId: Long) =
            songQuery.copy(uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId))

    fun observeGenreSongs(genreId: Long): Flow<List<Song>> =
            observeQuery(genreSongsQuery(genreId), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) { Song(it) }

    suspend fun loadGenreSongs(genreId: Long): List<Song> = load(genreSongsQuery(genreId)) { Song(it) }

    fun observeGenreAlbums(genreId: Long): Flow<List<Album>> = observeGenreSongs(genreId).map { songsToAlbums(it) }

    suspend fun getGenreOfSong(audioId: Long): Genre? =
            load(genreQuery.copy(uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", audioId.toInt()))) { Genre(it) }
                    .firstOrNull()

    // ---- Playlists (stored in Room; see UserPlaylist) ----

    private val playlistDao get() = localDataSource.playlistDao

    suspend fun addPlaylist(name: String): Long = playlistDao.insertPlaylist(UserPlaylist(name = name))

    suspend fun renamePlaylist(playlistId: Long, name: String) = playlistDao.renamePlaylist(playlistId, name)

    suspend fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    suspend fun addToPlaylist(playlistId: Long, songs: List<Song>?) {
        if (songs.isNullOrEmpty()) return
        playlistDao.appendSongs(playlistId, songs.map { it.id })
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = playlistDao.appendSongs(playlistId, listOf(songId))

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = playlistDao.removeSong(playlistId, songId)

    suspend fun getPlaylistSongIds(playlistId: Long): List<Long> = playlistDao.getSongIds(playlistId)

    suspend fun movePlaylistItem(playlistId: Long, fromPosition: Int, toPosition: Int) =
            playlistDao.moveItem(playlistId, fromPosition, toPosition)

    suspend fun removePlaylistItem(playlistId: Long, itemId: Long) = playlistDao.removeItem(playlistId, itemId)

    /** Smart playlists (most/recently played, recently added) followed by the user's playlists. */
    fun observePlaylists(): Flow<List<Playlist>> = playlistDao.observePlaylists().map { userPlaylists ->
        listOf(Playlist(Playlist.MOST_PLAYED), Playlist(Playlist.RECENTLY_PLAYED), Playlist(Playlist.RECENTLY_ADDED)) +
                userPlaylists.map { p -> Playlist(Playlist.USER, p.id, p.name).also { it.songCount = p.songCount } }
    }

    suspend fun loadPlaylistsExcept(playlistId: Long): List<Playlist> =
            playlistDao.getPlaylistsExcept(playlistId).map { Playlist(Playlist.USER, it.id, it.name) }

    fun observePlaylistSongs(playlistId: Long, playlistType: Int): Flow<List<Song>> = when (playlistType) {
        Playlist.USER -> combine(playlistDao.observeItems(playlistId), changes(songQuery.uri)) { items, _ -> items }
                .map { items ->
                    if (items.isEmpty()) return@map emptyList()
                    val byId = queryList(songsWithIds(items.map { it.songId }.toSet())) { Song(it) }.associateBy { it.id }
                    // Keep play order and duplicates; songs deleted from the device drop out.
                    items.mapNotNull { item -> byId[item.songId]?.copy()?.also { it.playlistSongId = item.id } }
                }
                .flowOn(Dispatchers.IO)
        Playlist.MOST_PLAYED -> statsToSongs(localDataSource.observeMostPlayedSongs())
        Playlist.RECENTLY_PLAYED -> statsToSongs(localDataSource.observeRecentlyPlayedSongs())
        else -> observeRecentlyAdded()
    }

    suspend fun loadPlaylistSongs(playlistId: Long, playlistType: Int): List<Song> =
            observePlaylistSongs(playlistId, playlistType).first()

    /** Songs for play stats, in the stats' order (most played / most recent first). */
    private fun statsToSongs(stats: Flow<List<SongStat>>): Flow<List<Song>> =
            combine(stats, changes(songQuery.uri)) { s, _ -> s }
                    .map { statList ->
                        if (statList.isEmpty()) return@map emptyList()
                        val byId = queryList(songsWithIds(statList.map { it.id })) { Song(it) }.associateBy { it.id }
                        statList.mapNotNull { stat ->
                            byId[stat.id]?.also {
                                it.playcount = stat.playCount
                                it.lastplayed = stat.playTime.toInt()
                            }
                        }
                    }
                    .flowOn(Dispatchers.IO)

    private fun observeRecentlyAdded(): Flow<List<Song>> {
        val fourWeeks = 4 * (3600 * 24 * 7)
        val query = songQuery.copy(
                selection = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.DATE_MODIFIED + ">?",
                args = arrayOf((System.currentTimeMillis() / 1000 - fourWeeks).toString()),
                sort = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"
        )
        return observeQuery(query) { Song(it) }
    }

    /**
     * One-time import of the device's MediaStore playlists into the app database.
     * Returns the number of playlists imported.
     */
    @Suppress("DEPRECATION")
    suspend fun importMediaStorePlaylists(): Int = withContext(Dispatchers.IO) {
        val playlists = queryList(Query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME),
                null, null, MediaStore.Audio.Playlists.NAME)) { it.getLong(0) to (it.getString(1) ?: "") }
        for ((id, name) in playlists) {
            val songIds = queryList(Query(MediaStore.Audio.Playlists.Members.getContentUri("external", id),
                    arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
                    null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER)) { it.getLong(0) }
            playlistDao.createWithSongs(name, songIds)
        }
        playlists.size
    }

    // ---- Favorites ----

    fun observeFavorites(): Flow<List<Song>> =
            combine(localDataSource.observeFavorites(), changes(songQuery.uri)) { favorites, _ -> favorites }
                    .map { favorites ->
                        if (favorites.isEmpty()) emptyList()
                        else queryList(songsWithIds(favorites.map { it.id })) { Song(it) }
                    }
                    .flowOn(Dispatchers.IO)

    // ---- Folders ----

    suspend fun getRealPath(uri: Uri): String? = withContext(Dispatchers.IO) { DopeUtil.getRealPathFromURI(contentResolver, uri) }

    @Suppress("DEPRECATION") // DATA is deprecated but still populated for reading.
    private fun folderSongsQuery(path: String) = songQuery.copy(
            selection = songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? AND "
                    + MediaStore.Audio.Media.DATA + " NOT LIKE ?)",
            args = arrayOf("$path%", "$path%/%")
    )

    fun observeFolderSongs(path: String): Flow<List<Song>> = observeQuery(folderSongsQuery(path)) { Song(it) }

    suspend fun loadFolderSongs(path: String): List<Song> = load(folderSongsQuery(path)) { Song(it) }

    /**
     * Child folders of [dirPath] that contain songs (at any depth), with their song counts.
     * Built from MediaStore paths instead of listing the file system, which scoped storage restricts.
     */
    @Suppress("DEPRECATION")
    suspend fun getChildFolders(dirPath: String): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val prefix = dirPath.trimEnd('/') + "/"
        val counts = LinkedHashMap<String, Int>()
        val paths = queryList(songQuery.copy(projection = arrayOf(MediaStore.Audio.Media.DATA),
                selection = songQuery.selection + " AND " + MediaStore.Audio.Media.DATA + " LIKE ?",
                args = arrayOf("$prefix%"), sort = null)) { it.getString(0) }
        for (path in paths) {
            val rest = path?.removePrefix(prefix) ?: continue
            val slash = rest.indexOf('/')
            if (slash > 0) {
                val child = prefix + rest.substring(0, slash)
                counts[child] = (counts[child] ?: 0) + 1
            }
        }
        counts.toList()
    }

    @Suppress("DEPRECATION")
    suspend fun getFolderSongCount(path: String): Int = withContext(Dispatchers.IO) {
        contentResolver.query(songQuery.uri, arrayOf(MediaStore.Audio.Media._ID),
                songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? )",
                arrayOf("$path%"), null)?.use { it.count } ?: 0
    }
}
