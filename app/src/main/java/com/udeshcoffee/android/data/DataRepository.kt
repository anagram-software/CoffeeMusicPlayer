package com.udeshcoffee.android.data

import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.model.Favorite
import com.udeshcoffee.android.data.metabrainz.ListenBrainzAccount
import com.udeshcoffee.android.data.metabrainz.MusicBrainz
import com.udeshcoffee.android.data.metabrainz.TrackMetadata
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * App-owned data (favorites, play stats), plus the tag editor's MusicBrainz lookup. All functions
 * are main-safe.
 */
class DataRepository(private val localDataSource: LocalDataSource, private val listenBrainz: ListenBrainzAccount) {

    // Favorites
    suspend fun isFavorite(songId: Long): Boolean = localDataSource.isFavorite(songId)

    fun observeIsFavorite(songId: Long): Flow<Boolean> = localDataSource.observeIsFavorite(songId)

    /** Toggles the favorite state and returns the new state. */
    suspend fun toggleFavorite(songId: Long): Boolean =
            if (localDataSource.isFavorite(songId)) {
                localDataSource.deleteFavorite(Favorite(songId))
                false
            } else {
                localDataSource.addFavorite(Favorite(songId))
                true
            }

    suspend fun addToFavorites(songs: List<Song>) = localDataSource.addFavorites(songs.map { it.id })

    // Play count
    suspend fun increasePlayCount(song: Song) = localDataSource.increasePlayCount(song)

    // Metadata for the tag editor
    suspend fun searchTrackMetadata(title: String, artist: String): TrackMetadata? = MusicBrainz.lookupRecording(title, artist, listenBrainz.token)
}
