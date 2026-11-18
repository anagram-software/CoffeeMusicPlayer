package com.udeshcoffee.android.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.contentValuesOf
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.media.songsToArtists
import com.udeshcoffee.android.data.media.statsToSongs
import com.udeshcoffee.android.data.model.UserPlaylist
import com.udeshcoffee.android.extensions.toBrite
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.utils.DopeUtil
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Udathari on 12/19/2017.
 */
class MediaRepository constructor(
        context: Context,
        private val localDataSource: LocalDataSource
) {

    private val contentResolver = context.contentResolver
    private val briteContentResolver = contentResolver.toBrite()

    // Projections
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

    private val genreProjection = arrayOf(MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME)


    // Queries
    private val songQuery = Query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songProjection,
            "(" + MediaStore.Audio.Media.IS_MUSIC + "=1 OR " + MediaStore.Audio.Media.IS_PODCAST + "=1 )",
            null,
            MediaStore.Audio.Media.TITLE
    )

    private val genreQuery = Query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            genreProjection,
            null,
            null,
            MediaStore.Audio.Genres.NAME
    )

    // Functions
    // Songs
    fun getSong(uri: Uri): Observable<List<Song>> {
        val query = songQuery.copy(uri = uri)
        return briteContentResolver
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .mapToList { Song(it) }
                .take(1)
    }

    fun getSongs(): Observable<List<Song>> {
        return briteContentResolver.createQuery(songQuery.uri, songQuery.projection, songQuery.selection,
                songQuery.args, songQuery.sort, false)
                .mapToList { Song(it) }
    }


    // Albums
    fun getAlbums(): Observable<List<Album>> =
            getSongs().flatMap { Observable.just(songsToAlbums(it)) }

    fun getAlbumSongs(albumId: Long): Observable<List<Song>> {
        return getSongs()
                .map { songs ->
                    val songs1 = ArrayList<Song>()
                    Stream.of(songs)
                            .forEach { song ->
                                if (song.albumId == albumId)
                                    songs1.add(song)
                            }
                    return@map songs1
                }
    }


    // Artists
    fun getArtists(): Observable<List<Artist>> =
            getSongs().flatMap { Observable.just(songsToArtists(it)) }

    fun getArtistSongs(artistId: Long): Observable<List<Song>> {
        return getSongs()
                .map { songs ->
                    val songs1 = ArrayList<Song>()
                    Stream.of(songs)
                            .forEach { song ->
                                if (song.artistId == artistId)
                                    songs1.add(song)
                            }
                    return@map songs1
                }
    }

    fun getArtistAlbums(artistName: String): Observable<List<Album>> {
        return getAlbums()
                .map { albums ->
                    val albums1 = ArrayList<Album>()
                    Stream.of(albums)
                            .forEach { album ->
                                if (album.artist == artistName)
                                    albums1.add(album)
                            }
                    return@map albums1
                }
    }

    // Genres
    fun getGenres(): Observable<List<Genre>> {
        return briteContentResolver
                .createQuery(genreQuery.uri, genreQuery.projection, genreQuery.selection, genreQuery.args, genreQuery.sort, false)
                .mapToList { Genre(it) }
    }

    fun getGenreSongs(genreId: Long): Observable<List<Song>> {
        val query = songQuery.copy(
                uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
        )
        return contentResolver.toBrite()
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .mapToList { Song(it) }
    }

    fun getGenreAlbums(genreId: Long): Observable<List<Album>> =
            getGenreSongs(genreId).flatMap { Observable.just(songsToAlbums(it)) }

    fun getGenreOfSong(audioId: Long): Genre? {
        val query = genreQuery.copy(
                uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", audioId.toInt())
        )
        var genre: Genre? = null
        val cursor = contentResolver.query(query.uri, query.projection, query.selection, query.args, query.sort)
        cursor?.let {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                genre = Genre(cursor)
            }
            cursor.close()
        }
        return genre
    }

    private fun getGenreSongCount(genreId: Long): Int? {
        val cursor = contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                arrayOf(MediaStore.Audio.Media.TITLE),
                null,
                null,
                null)
        var count = 0
        if (cursor != null) {
            cursor.moveToFirst()
            count = cursor.count

            cursor.close()
        }
        return count
    }

    fun cleanGenres(): Disposable {
        return getGenres()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .take(1)
                .delaySubscription(2500, TimeUnit.MILLISECONDS)
                .flatMapIterable { it }
                .subscribe {
                    val count = getGenreSongCount(it.id)
                    Log.d("cleanGenres", "subscribe ${it.name} $count")
                    if (count == 0) {
                        Log.d("cleanGenres", "delete ${it.name}")
                        try {
                            contentResolver.delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                                    MediaStore.Audio.Genres._ID + " == " + it.id, null)
                        } catch (e: Exception) {
                            //Don't care if we couldn't delete this uri.
                        }
                    }
                }
    }

    // Playlist (stored in Room; see UserPlaylist)
    private val playlistDao get() = localDataSource.playlistDao

    fun addPlaylist(name: String): Long = playlistDao.insertPlaylist(UserPlaylist(name = name))

    fun renamePlaylist(playlistId: Long, name: String) = playlistDao.renamePlaylist(playlistId, name)

    fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    fun addToPlaylist(playlistId: Long, songs: List<Song>?) {
        if (songs.isNullOrEmpty()) return
        playlistDao.appendSongs(playlistId, songs.map { it.id })
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) = playlistDao.appendSongs(playlistId, listOf(songId))

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) = playlistDao.removeSong(playlistId, songId)

    fun getPlaylistSongIds(playlistId: Long): List<Long> = playlistDao.getSongIds(playlistId)

    fun getPlaylists(): Observable<List<Playlist>> {
        return playlistDao.observePlaylists()
                .toObservable()
                .map { userPlaylists ->
                    val list = arrayListOf(
                            Playlist(Playlist.MOST_PLAYED),
                            Playlist(Playlist.RECENTLY_PLAYED),
                            Playlist(Playlist.RECENTLY_ADDED))
                    userPlaylists.mapTo(list) { p ->
                        Playlist(Playlist.USER, p.id, p.name).also { it.songCount = p.songCount }
                    }
                    list as List<Playlist>
                }
    }

    fun getPlaylistsExcept(playlistId: Long): List<Playlist> =
            playlistDao.getPlaylistsExcept(playlistId).map { Playlist(Playlist.USER, it.id, it.name) }

    fun getPlaylistSongs(playlistId: Long, playlistType: Int): Observable<List<Song>>? {
        when (playlistType) {
            Playlist.USER -> {
                return playlistDao.observeItems(playlistId)
                        .toObservable()
                        .switchMap { items ->
                            if (items.isEmpty()) return@switchMap Observable.just(emptyList<Song>())
                            val query = songQuery.copy(
                                    selection = songQuery.selection + " AND " + MediaStore.Audio.Media._ID +
                                            " IN (" + items.map { it.songId }.distinct().joinToString(",") + ")")
                            briteContentResolver
                                    .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                                    .mapToList { Song(it) }
                                    .map { songs ->
                                        val byId = songs.associateBy { it.id }
                                        // Keep play order and duplicates; songs deleted from the device drop out.
                                        items.mapNotNull { item ->
                                            byId[item.songId]?.copy()?.also { it.playlistSongId = item.id }
                                        }
                                    }
                        }
            }
            Playlist.MOST_PLAYED -> return localDataSource.getMostPlayedSongs()
                    .flatMap { stats ->
                        statsToSongs(stats, briteContentResolver, songQuery)
                                .map { songs ->
                                    return@map songs.sortedWith(kotlin.Comparator { a, b -> b.playcount - a.playcount })
                                }
                    }
            Playlist.RECENTLY_PLAYED -> {
                return localDataSource.getRecentlyPlayedSongs()
                        .flatMap { stats ->
                            statsToSongs(stats, briteContentResolver, songQuery)
                                    .map { songs ->
                                        return@map songs.sortedWith(kotlin.Comparator { a, b -> b.playcount - a.playcount })
                                    }
                        }
            }
            Playlist.RECENTLY_ADDED -> {
                return getRecentlyAdded()
            }
        }
        return null
    }

    /**
     * One-time import of the device's MediaStore playlists into the app database.
     * Returns the number of playlists imported.
     */
    @Suppress("DEPRECATION")
    fun importMediaStorePlaylists(): Int {
        var imported = 0
        val playlists = ArrayList<Pair<Long, String>>()
        contentResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME),
                null, null, MediaStore.Audio.Playlists.NAME)?.use { c ->
            while (c.moveToNext()) playlists.add(c.getLong(0) to (c.getString(1) ?: ""))
        }
        for ((id, name) in playlists) {
            val songIds = ArrayList<Long>()
            contentResolver.query(MediaStore.Audio.Playlists.Members.getContentUri("external", id),
                    arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
                    null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER)?.use { c ->
                while (c.moveToNext()) songIds.add(c.getLong(0))
            }
            playlistDao.createWithSongs(name, songIds)
            imported++
        }
        return imported
    }

    private fun getRecentlyAdded(): Observable<List<Song>> {
        val numWeeks = 4 * (3600 * 24 * 7)
        val query = songQuery.copy(
                selection = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.DATE_MODIFIED + ">?",
                args = arrayOf((System.currentTimeMillis() / 1000 - numWeeks).toString() + ""),
                sort = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"
        )
        return briteContentResolver
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .mapToList { Song(it) }
    }

    fun movePlaylistItem(playlistId: Long, fromPosition: Int, toPosition: Int) =
            playlistDao.moveItem(playlistId, fromPosition, toPosition)

    fun removePlayistItem(playlistId: Long, itemId: Long) = playlistDao.removeItem(playlistId, itemId)

    // Favorites
    fun getFavorites(): Flowable<List<Song>> {
        return localDataSource.getFavorites()
                .flatMap { favorites ->
                    val songsQuery = songQuery.copy(
                            selection = songQuery.selection + " AND " + MediaStore.Audio.Media._ID + " IN (" +
                                    Stream.of(favorites)
                                            .map { it.id.toString() }
                                            .collect(Collectors.joining(",")) +
                                    ")"
                    )
                    return@flatMap contentResolver.toBrite()
                            .createQuery(songsQuery.uri, songsQuery.projection, songsQuery.selection, songsQuery.args,
                                    songsQuery.sort, false)
                            .mapToList { Song(it) }
                            .toFlowable(BackpressureStrategy.BUFFER)
                }
    }

    // Folders
    fun getRealPath(uri: Uri): String? = DopeUtil.getRealPathFromURI(contentResolver, uri)

    fun getFolderSongs(path: String): Observable<List<Song>> {
        val query = songQuery.copy(
                selection = songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? AND "
                        + MediaStore.Audio.Media.DATA + " NOT LIKE ?)",
                args = arrayOf("$path%", "$path%/%")
        )
        return briteContentResolver
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .mapToList { Song(it) }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Child folders of [dirPath] that contain songs (at any depth), with their song counts.
     * Built from MediaStore paths instead of listing the file system, which scoped storage restricts.
     */
    @Suppress("DEPRECATION") // DATA is deprecated but still populated for reading.
    fun getChildFolders(dirPath: String): List<Pair<String, Int>> {
        val prefix = dirPath.trimEnd('/') + "/"
        val counts = LinkedHashMap<String, Int>()
        contentResolver.query(songQuery.uri, arrayOf(MediaStore.Audio.Media.DATA),
                songQuery.selection + " AND " + MediaStore.Audio.Media.DATA + " LIKE ?",
                arrayOf("$prefix%"), null)?.use { c ->
            while (c.moveToNext()) {
                val rest = c.getString(0)?.removePrefix(prefix) ?: continue
                val slash = rest.indexOf('/')
                if (slash > 0) {
                    val child = prefix + rest.substring(0, slash)
                    counts[child] = (counts[child] ?: 0) + 1
                }
            }
        }
        return counts.toList()
    }

    fun getFolderSongCount(path: String): Int {
        val query = songQuery.copy(
                projection = arrayOf(MediaStore.Audio.Media._ID),
                selection = songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? )",
                args = arrayOf("$path%")
        )
        val audioCursor = contentResolver.query(query.uri, query.projection, query.selection, query.args, query.sort)
        return if (audioCursor != null && audioCursor.moveToFirst()) {
            val count = audioCursor.count
            audioCursor.close()
            count
        } else {
            0
        }
    }

}