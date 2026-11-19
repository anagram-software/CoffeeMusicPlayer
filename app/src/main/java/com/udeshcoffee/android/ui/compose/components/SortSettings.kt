package com.udeshcoffee.android.ui.compose.components

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.SortManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Current sort order and direction. */
data class SortState(val order: Int, val ascending: Boolean)

/** A persisted sort preference (stored by [SortManager]) exposed as a StateFlow. */
class SortSetting(
        private val readOrder: () -> Int,
        private val writeOrder: (Int) -> Unit,
        private val readAscending: () -> Boolean,
        private val writeAscending: (Boolean) -> Unit
) {
    private val flow = MutableStateFlow(SortState(readOrder(), readAscending()))
    val state: StateFlow<SortState> = flow.asStateFlow()

    fun setOrder(order: Int) {
        writeOrder(order)
        flow.value = flow.value.copy(order = order)
    }

    fun setAscending(ascending: Boolean) {
        writeAscending(ascending)
        flow.value = flow.value.copy(ascending = ascending)
    }

    fun group(title: String?, options: List<SortOption>, state: SortState) =
            SortGroup(title, options, state.order, state.ascending, ::setOrder, ::setAscending)
}

object SortSettings {
    val songs = SortSetting({ SortManager.songsSortOrder }, { SortManager.songsSortOrder = it }, { SortManager.songsAscending }, { SortManager.songsAscending = it })
    val albums = SortSetting({ SortManager.albumsSortOrder }, { SortManager.albumsSortOrder = it }, { SortManager.albumsAscending }, { SortManager.albumsAscending = it })
    val artists = SortSetting({ SortManager.artistSortOrder }, { SortManager.artistSortOrder = it }, { SortManager.artistsAscending }, { SortManager.artistsAscending = it })
    val albumSongs = SortSetting({ SortManager.albumSongsSortOrder }, { SortManager.albumSongsSortOrder = it }, { SortManager.albumSongsAscending }, { SortManager.albumSongsAscending = it })
    val artistSongs = SortSetting({ SortManager.artistSongsSortOrder }, { SortManager.artistSongsSortOrder = it }, { SortManager.artistSongsAscending }, { SortManager.artistSongsAscending = it })
    val artistAlbums = SortSetting({ SortManager.artistAlbumsSortOrder }, { SortManager.artistAlbumsSortOrder = it }, { SortManager.artistAlbumsAscending }, { SortManager.artistAlbumsAscending = it })
    val genreSongs = SortSetting({ SortManager.genreSongsSortOrder }, { SortManager.genreSongsSortOrder = it }, { SortManager.genreSongsAscending }, { SortManager.genreSongsAscending = it })
    val genreAlbums = SortSetting({ SortManager.genreAlbumsSortOrder }, { SortManager.genreAlbumsSortOrder = it }, { SortManager.genreAlbumsAscending }, { SortManager.genreAlbumsAscending = it })
    val favoriteSongs = SortSetting({ SortManager.favoritesSongsSortOrder }, { SortManager.favoritesSongsSortOrder = it }, { SortManager.favoritesSongsAscending }, { SortManager.favoritesSongsAscending = it })
}

private fun <T> List<T>.sortedBy(state: SortState, sort: (List<T>, Int) -> Unit): List<T> =
        toMutableList().also { sort(it, state.order); if (!state.ascending) it.reverse() }

fun List<Song>.sortedSongs(state: SortState): List<Song> = sortedBy(state, SortManager::sortSongs)
fun List<Album>.sortedAlbums(state: SortState): List<Album> = sortedBy(state, SortManager::sortAlbums)
fun List<Artist>.sortedArtists(state: SortState): List<Artist> = sortedBy(state, SortManager::sortArtists)
