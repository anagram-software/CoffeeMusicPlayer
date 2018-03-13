package com.udeshcoffee.android.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.media.*
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.extensions.toBrite
import com.udeshcoffee.android.utils.DopeUtil
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Udathari on 12/19/2017.
 */
class MediaRepository constructor (
        context: Context,
        private val localDataSource: LocalDataSource
){

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


    private val playlistProjection = arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME)

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

    private val playlistQuery = Query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            playlistProjection,
            null,
            null,
            MediaStore.Audio.Playlists.NAME)

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

    fun getSongCount(): Observable<Int> = getSongs().map { it.size }

    // Albums
    fun getAlbums(): Observable<List<Album>> =
            getSongs().flatMap({ Observable.just(songsToAlbums(it)) })

    fun getAlbumSongs(albumId: Long): Observable<List<Song>> {
        return getSongs()
                .map({ songs ->
                    val songs1 = ArrayList<Song>()
                    Stream.of(songs)
                            .forEach { song ->
                                if (song.albumId == albumId)
                                    songs1.add(song)
                            }
                    return@map songs1
                })
    }

    fun getAlbumCount(): Observable<Int> = getAlbums().map { it.size }

    // Artists
    fun getArtists(): Observable<List<Artist>> =
            getSongs().flatMap({ Observable.just(songsToArtists(it)) })

    fun getArtistSongs(artistId: Long): Observable<List<Song>> {
        return getSongs()
                .map({ songs ->
                    val songs1 = ArrayList<Song>()
                    Stream.of(songs)
                            .forEach { song ->
                                if (song.artistId == artistId)
                                    songs1.add(song)
                            }
                    return@map songs1
                })
    }

    fun getArtistAlbums(artistName: String): Observable<List<Album>> {
        return getAlbums()
                .map({ albums ->
                    val albums1 = ArrayList<Album>()
                    Stream.of(albums)
                            .forEach { album ->
                                if (album.artist == artistName)
                                    albums1.add(album)
                            }
                    return@map albums1
                })
    }

    fun getArtistCount(): Observable<Int> = getArtists().map { it.size }

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
                .mapToList{ Song(it) }
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

    fun getGenreSongCount(genreId: Long): Observable<Int>? {
        val cursor = contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                null,
                null,
                null)
        var count: Observable<Int>? = null
        if (cursor != null) {
            count = Observable.just(cursor.count)

            cursor.close()
        }
        return count
    }

    fun cleanGenres() {
        getGenres()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .take(1)
                .flatMapIterable{it -> it}
                .concatMap{Observable.just(it).delay(250, TimeUnit.MILLISECONDS)}
                // Since this is called on app launch, let's delay to allow more important tasks to complete.
                .delaySubscription(2500, TimeUnit.MILLISECONDS)
                .subscribe{
                    getGenreSongCount(it.id)
                            ?.firstOrError()
                            ?.subscribe{ numSongs ->
                                if (numSongs == 0) {
                                    try {
                                        contentResolver.delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                                                MediaStore.Audio.Genres._ID + " == " + it.id, null);
                                    } catch (e: Exception) {
                                        //Don't care if we couldn't delete this uri.
                                    }
                                }
                            }
                }
    }

    // Playlist
    fun addToPlaylist(playlistId: Long, songs: ArrayList<Song>?) {
        for (i in songs!!.indices) {
            val resolver = contentResolver
            val cols = arrayOf("count(*)")
            val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
            val cur = resolver.query(uri, cols, null, null, null)
            cur!!.moveToFirst()
            val base = cur.getInt(0)
            cur.close()
            val values = ContentValues()
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base)
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs[i].id)
            resolver.insert(uri, values)
        }
    }

    fun getPlaylists(): Observable<List<Playlist>> {
        return briteContentResolver
                .createQuery(playlistQuery.uri, playlistQuery.projection, playlistQuery.selection,
                        playlistQuery.args, playlistQuery.sort, false)
                .mapToList { cursor: Cursor -> Playlist(cursor) }
                .flatMap { Observable.just(toCompletePlaylists(contentResolver, it)) }
    }

    fun getPlaylistsExcept(playlistId: Long): List<Playlist> {
        val items = ArrayList<Playlist>()
        contentResolver
                .query(playlistQuery.uri,
                        playlistQuery.projection,
                        playlistQuery.selection,
                        playlistQuery.args,
                        playlistQuery.sort)?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val item = Playlist(cursor)
                    if (playlistId != item.id)
                        items.add(item)
                } while (cursor.moveToNext())
            }
        }
        return items
    }

    fun getPlaylistCount(): Observable<Int> {
        return briteContentResolver
                .createQuery(playlistQuery.uri, playlistQuery.projection, playlistQuery.selection,
                        playlistQuery.args, playlistQuery.sort, false)
                .mapToList { cursor: Cursor -> Playlist(cursor) }
                .map { it.size }
    }

    fun getPlaylistSongs(playlistId: Long, playlistType: Int): Observable<List<Song>>? {
        when (playlistType) {
            Playlist.USER -> {
                val query = songQuery.copy(
                        uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                        projection = arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID,
                                MediaStore.Audio.Playlists.Members._ID,
                                MediaStore.Audio.Playlists.Members.TITLE,
                                MediaStore.Audio.Playlists.Members.ARTIST_ID,
                                MediaStore.Audio.Playlists.Members.ARTIST,
                                MediaStore.Audio.Playlists.Members.ALBUM_ID,
                                MediaStore.Audio.Playlists.Members.ALBUM,
                                MediaStore.Audio.Playlists.Members.DURATION,
                                MediaStore.Audio.Playlists.Members.YEAR,
                                MediaStore.Audio.Playlists.Members.DATE_ADDED,
                                MediaStore.Audio.Playlists.Members.DATE_MODIFIED,
                                MediaStore.Audio.Playlists.Members.TRACK),
                        sort = MediaStore.Audio.Playlists.Members.PLAY_ORDER

                )
                return briteContentResolver
                        .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                        .mapToList(Playlist.SONGMAPPER)
            }
            Playlist.MOST_PLAYED -> return localDataSource.getMostPlayedSongs()
                    .flatMap({ stats -> statsToSongs(stats, briteContentResolver, songQuery)
                                .map({ songs ->
                                    Collections.sort(songs) { a, b -> b.playcount - a.playcount}
                                    songs
                                })
                    })
            Playlist.RECENTLY_PLAYED -> {
                return localDataSource.getRecentlyPlayedSongs()
                        .flatMap({ stats -> statsToSongs(stats, briteContentResolver, songQuery)
                                    .map({ songs ->
                                        Collections.sort(songs) { a, b -> b.lastplayed - a.lastplayed }
                                        songs
                                    })
                        })
            }
            Playlist.RECENTLY_ADDED -> {
                return getRecentlyAdded()
            }
        }
        return null
    }

    fun getRecentlyAdded(limit: Int): Observable<List<Song>> {
        return getRecentlyAdded().map {
            val min = Math.min(limit, it.size)
            it.subList(0, min)
        }
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
                .mapToList{Song(it)}
    }

    fun movePlaylistItem(playlistId: Long, fromPosition: Int, toPosition: Int) {
        try {
            MediaStore.Audio.Playlists.Members.moveItem(contentResolver, playlistId, fromPosition, toPosition)
        } catch (e: IllegalArgumentException) {

        }
    }

    fun removePlayistItem(playlistId: Long, itemId: Long) {
        try {
            val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
            contentResolver.delete(ContentUris.withAppendedId(uri, itemId), null, null)
        } catch (e: IllegalArgumentException) {

        }
    }

    // Favorites
    fun getFavorites(): Flowable<List<Song>> {
        return localDataSource.getFavorites()
                .flatMap{favorites ->
                    val songsQuery = songQuery.copy(
                            selection = songQuery.selection + " AND " + MediaStore.Audio.Media._ID + " IN (" +
                                    Stream.of(favorites)
                                            .map{it.id.toString()}
                                            .collect(Collectors.joining(",")) +
                                    ")"
                    )
                    return@flatMap contentResolver.toBrite()
                            .createQuery(songsQuery.uri, songsQuery.projection, songsQuery.selection, songsQuery.args,
                                    songsQuery.sort, false)
                            .mapToList{Song(it)}
                            .toFlowable(BackpressureStrategy.BUFFER)
                }
    }

    // Folders
    fun getRealPath(uri: Uri): String? = DopeUtil.getRealPathFromURI(contentResolver, uri)

    fun getFolderSongs(path: String): Observable<List<Song>> {
        val query = songQuery.copy(
                selection = songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? AND "
                        + MediaStore.Audio.Media.DATA + " NOT LIKE ?)",
                args = arrayOf(path + "%", path + "%/%")
        )
        return briteContentResolver
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .mapToList{Song(it)}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getFolderSongCount(path: String): Int {
        val query = songQuery.copy(
                projection = arrayOf(MediaStore.Audio.Media._ID),
                selection = songQuery.selection + " AND (" + MediaStore.Audio.Media.DATA + " LIKE ? )",
                args = arrayOf(path + "%")
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

    // Stats
    fun getTopSongs(): Observable<List<Song>> {
        return localDataSource.getTopSongs()
                .flatMap({ stats -> statsToSongs(stats, briteContentResolver, songQuery)})
                .map({ songs ->
                    Collections.sort(songs) { a, b -> b.playcount - a.playcount}
                    songs
                })
    }

    fun getTopAlbums(): Observable<List<Album>> {
        return localDataSource.getTopAlbums()
                .flatMap {
                    getAlbums().map { it1 -> statsToAlbums(it, it1) }
                }
    }

    fun getTopArtists(): Observable<List<Artist>> {
        return localDataSource.getTopArtists()
                .flatMap {
                    getArtists().map { it1 -> statsToArtists(it, it1) }
                }
    }

}