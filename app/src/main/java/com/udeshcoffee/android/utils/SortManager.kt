package com.udeshcoffee.android.utils

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.App
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.Song
import java.util.*

/**
* Created by Udathari on 7/25/2017.
*/
object SortManager {

    private val mPrefs : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.instance)

    object SortType {
        const val Song = 0
        const val Album = 1
        const val Artist = 2
    }

    // Song sort typedef
    object SongSort {
        const val DEFAULT = 0
        const val NAME = 1
        const val TRACK_NUMBER = 2
        const val DURATION = 3
        const val DATE = 4
        const val YEAR = 5
        const val ALBUM_NAME = 6
        const val ARTIST_NAME = 7
        const val ALBUM_DEFAULT = 8
    }

    // Album sort typedef
    object AlbumSort {
        const val DEFAULT = 0
        const val NAME = 1
        const val YEAR = 2
        const val ARTIST_NAME = 3
    }

    // Album sort typedef
    object ArtistSort {
        const val DEFAULT = 0
        const val NAME = 1
    }

    // Folder sort typedef
    object FolderSort {
        const val DEFAULT = 0
        const val NAME = 1
        const val SONG_COUNT = 2
    }

    object Key {
        const val ARTISTS = "key_artists_sort_order"
        const val ALBUMS = "key_albums_sort_order"
        const val SONGS = "key_songs_sort_order"
        const val FOLDERS = "key_folders_sort_order"
        const val ARTISTS_ASC = "key_artists_sort_order_asc"
        const val ALBUMS_ASC = "key_albums_sort_order_asc"
        const val SONGS_ASC = "key_songs_sort_order_asc"
        const val FOLDERS_ASC = "key_folders_sort_order_asc"

        const val ALBUM_SONGS= "key_album_songs_sort_order"
        const val ALBUM_SONGS_ASC= "key_album_songs_sort_order_asc"

        const val ARTIST_SONGS= "key_artist_songs_sort_order"
        const val ARTIST_ALBUMS= "key_artist_albums_sort_order"
        const val ARTIST_SONGS_ASC= "key_artist_songs_sort_order_asc"
        const val ARTIST_ALBUMS_ASC= "key_artist_albums_sort_order_asc"

        const val GENRE_SONGS= "key_genre_songs_sort_order"
        const val GENRE_ALBUMS= "key_genre_albums_sort_order"
        const val GENRE_SONGS_ASC= "key_genre_songs_sort_order_asc"
        const val GENRE_ALBUMS_ASC= "key_genre_albums_sort_order_asc"

        const val FOLDER_SONGS= "key_folder_songs_sort_order"
        const val FOLDER_SONGS_ASC= "key_folder_songs_sort_order_asc"

        const val FAVORITES_SONGS= "key_fav_songs_sort_order"
        const val FAVORITES_SONGS_ASC= "key_fav_songs_sort_order_asc"
    }

    var songsSortOrder : Int
        get() {return mPrefs.getInt(Key.SONGS, SongSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.SONGS, value).apply()
        }

    var albumsSortOrder : Int
        get() {return mPrefs.getInt(Key.ALBUMS, ArtistSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.ALBUMS, value).apply()
        }

    var artistSortOrder : Int
        get() {return mPrefs.getInt(Key.ARTISTS, ArtistSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.ARTISTS, value).apply()
        }

    var folderSortOrder : Int
        get() {return mPrefs.getInt(Key.FOLDERS, FolderSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.FOLDERS, value).apply()
        }

    var albumSongsSortOrder : Int
        get() {return mPrefs.getInt(Key.ALBUM_SONGS, SongSort.ALBUM_DEFAULT) }
        set(value) {
            mPrefs.edit().putInt(Key.ALBUM_SONGS, value).apply()
        }

    var artistSongsSortOrder : Int
        get() {return mPrefs.getInt(Key.ARTIST_SONGS, SongSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.ARTIST_SONGS, value).apply()
        }

    var artistAlbumsSortOrder : Int
        get() {return mPrefs.getInt(Key.ARTIST_ALBUMS, AlbumSort.YEAR) }
        set(value) {
            mPrefs.edit().putInt(Key.ARTIST_ALBUMS, value).apply()
        }

    var genreSongsSortOrder : Int
        get() {return mPrefs.getInt(Key.GENRE_SONGS, SongSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.GENRE_SONGS, value).apply()
        }

    var genreAlbumsSortOrder : Int
        get() {return mPrefs.getInt(Key.GENRE_ALBUMS, AlbumSort.YEAR) }
        set(value) {
            mPrefs.edit().putInt(Key.GENRE_ALBUMS, value).apply()
        }

    var folderSongsSortOrder : Int
        get() {return mPrefs.getInt(Key.FOLDER_SONGS, SongSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.FOLDER_SONGS, value).apply()
        }

    var favoritesSongsSortOrder : Int
        get() {return mPrefs.getInt(Key.FAVORITES_SONGS, SongSort.NAME) }
        set(value) {
            mPrefs.edit().putInt(Key.FAVORITES_SONGS, value).apply()
        }

    var songsAscending : Boolean
        get() = mPrefs.getBoolean(Key.SONGS_ASC, true)
        set(value) {
            setAscending(Key.SONGS_ASC, value)
        }

    var albumsAscending : Boolean
        get() = mPrefs.getBoolean(Key.ALBUMS_ASC, true)
        set(value) {
            setAscending(Key.ALBUMS_ASC, value)
        }

    var artistsAscending : Boolean
        get() = mPrefs.getBoolean(Key.ARTISTS_ASC, true)
        set(value) {
            setAscending(Key.ARTISTS_ASC, value)
        }

    var foldersAscending : Boolean
        get() = mPrefs.getBoolean(Key.FOLDERS_ASC, true)
        set(value) {
            setAscending(Key.FOLDERS_ASC, value)
        }

    var albumSongsAscending : Boolean
        get() = mPrefs.getBoolean(Key.ALBUM_SONGS_ASC, true)
        set(value) {
            setAscending(Key.ALBUM_SONGS_ASC, value)
        }

    var artistSongsAscending : Boolean
        get() = mPrefs.getBoolean(Key.ARTIST_SONGS_ASC, true)
        set(value) {
            setAscending(Key.ARTIST_SONGS_ASC, value)
        }

    var artistAlbumsAscending : Boolean
        get() = mPrefs.getBoolean(Key.ARTIST_ALBUMS_ASC, true)
        set(value) {
            setAscending(Key.ARTIST_ALBUMS_ASC, value)
        }

    var genreSongsAscending : Boolean
        get() = mPrefs.getBoolean(Key.GENRE_SONGS_ASC, true)
        set(value) {
            setAscending(Key.GENRE_SONGS_ASC, value)
        }

    var genreAlbumsAscending : Boolean
        get() = mPrefs.getBoolean(Key.GENRE_ALBUMS_ASC, true)
        set(value) {
            setAscending(Key.GENRE_ALBUMS_ASC, value)
        }

    var folderSongsAscending : Boolean
        get() = mPrefs.getBoolean(Key.FOLDER_SONGS_ASC, true)
        set(value) {
            setAscending(Key.FOLDER_SONGS_ASC, value)
        }

    var favoritesSongsAscending : Boolean
        get() = mPrefs.getBoolean(Key.FAVORITES_SONGS_ASC, true)
        set(value) {
            setAscending(Key.FAVORITES_SONGS_ASC, value)
        }

    // Album sorts
    fun sortAlbums(albums: List<Album>) {
        sortAlbums(albums, albumsSortOrder)
    }

    fun sortArtistAlbums(albums: List<Album>) {
        sortAlbums(albums, artistAlbumsSortOrder)
    }

    fun sortGenreAlbums(albums: List<Album>) {
        sortAlbums(albums, genreAlbumsSortOrder)
    }

    private fun sortAlbums(albums: List<Album>, key: Int) {
        when (key) {
            AlbumSort.DEFAULT -> Collections.sort(albums) { a, b -> a.compareTo(b)}
            AlbumSort.NAME -> {
                Collections.sort(albums) { a, b -> compareString(a.title, b.title) }
            }
            AlbumSort.YEAR -> Collections.sort(albums) { a, b -> compareInt(b.year, a.year) }
            AlbumSort.ARTIST_NAME -> Collections.sort(albums) { a, b -> compareString(a.artist, b.artist) }
        }
    }

    // SOng sorts
    fun sortSongs(songs: List<Song>) {
        sortSongs(songs, songsSortOrder)
    }

    fun sortAlbumSongs(songs: List<Song>) {
        sortSongs(songs, albumSongsSortOrder)
    }

    fun sortArtistSongs(songs: List<Song>) {
        sortSongs(songs, artistSongsSortOrder)
    }

    fun sortGenreSongs(songs: List<Song>) {
        sortSongs(songs, genreSongsSortOrder)
    }

    fun sortFolderSongs(songs: List<Song>) {
        sortSongs(songs, folderSongsSortOrder)
    }

    fun sortFavoritesSongs(songs: List<Song>) {
        sortSongs(songs, favoritesSongsSortOrder)
    }

    private fun sortSongs(songs: List<Song>, key: Int) {
        when (key) {
            SongSort.DEFAULT -> Collections.sort(songs) { a, b -> a.compareTo(b) }
            SongSort.ALBUM_DEFAULT -> {
                Collections.sort(songs) { a, b -> compareInt(a.track, b.track) }
                Collections.sort(songs) { a, b -> compareInt(a.discNumber, b.discNumber) }
            }
            SongSort.NAME -> Collections.sort(songs) { a, b -> compareString(a.title, b.title) }
            SongSort.TRACK_NUMBER -> {
                Collections.sort(songs) { a, b -> compareInt(a.track, b.track) }
                Collections.sort(songs) { a, b -> compareInt(a.discNumber, b.discNumber) }
            }
            SongSort.DURATION -> Collections.sort(songs) { a, b -> compareLong(a.duration, b.duration) }
            SongSort.DATE -> Collections.sort(songs) { a, b -> compareInt(b.dateAdded, a.dateAdded) }
            SongSort.YEAR -> {
                Collections.sort(songs) { a, b -> compareString(a.artistName, b.artistName) }
                Collections.sort(songs) { a, b -> compareString(a.albumName, b.albumName) }
                Collections.sort(songs) { a, b -> a.compareTo(b) }
                Collections.sort(songs) { a, b -> compareInt(b.year, a.year) }
            }
            SongSort.ALBUM_NAME -> {
                Collections.sort(songs) { a, b -> compareString(a.artistName, b.artistName) }
                Collections.sort(songs) { a, b -> compareInt(a.track, b.track) }
                Collections.sort(songs) { a, b -> compareInt(a.discNumber, b.discNumber) }
                Collections.sort(songs) { a, b -> compareString(a.albumName, b.albumName) }
            }
            SongSort.ARTIST_NAME -> {
                Collections.sort(songs) { a, b -> compareString(a.albumName, b.albumName) }
                Collections.sort(songs) { a, b -> compareInt(a.track, b.track) }
                Collections.sort(songs) { a, b -> compareInt(a.discNumber, b.discNumber) }
                Collections.sort(songs) { a, b -> compareString(a.artistName, b.artistName) }
            }
        }
    }

    fun sortArtists(artists: List<Artist>) {
        when (artistSortOrder) {
            ArtistSort.DEFAULT -> Collections.sort(artists) { a, b -> a.compareTo(b) }
            ArtistSort.NAME -> Collections.sort(artists) { a, b -> compareString(a.name, b.name) }
        }
    }

    fun sortFolders(folders: List<Folder>) {
        when (folderSortOrder) {
            FolderSort.DEFAULT -> Collections.sort(folders) { a, b -> compareString(a.title, b.title) }
            FolderSort.NAME -> Collections.sort(folders) { a, b -> compareString(a.title, b.title) }
            FolderSort.SONG_COUNT -> Collections.sort(folders) { a, b -> compareInt(a.songCount, b.songCount) }
        }
    }

    private fun setAscending(key: String, ascending: Boolean) {
        mPrefs.edit().putBoolean(key, ascending).apply()
    }
}