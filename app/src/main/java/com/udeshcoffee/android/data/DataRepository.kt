package com.udeshcoffee.android.data

import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.model.Favorite
import com.udeshcoffee.android.data.model.Lyric
import com.udeshcoffee.android.data.remote.RemoteDataSource
import com.udeshcoffee.android.data.remote.genius.Hit
import com.udeshcoffee.android.data.remote.lastfm.TrackMetadata
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * App-owned data (lyrics, favorites, play stats, artist bios), combining the local
 * database with network sources. All functions are main-safe.
 */
class DataRepository(
        private val localDataSource: LocalDataSource,
        private val remoteDataSource: RemoteDataSource
) {

    // Lyrics
    suspend fun getLyrics(song: Song): String? = localDataSource.getLyrics(song.id)

    suspend fun searchLyrics(title: String, artist: String, exactTitle: Boolean): List<Hit> =
            remoteDataSource.searchGenius(title, artist, exactTitle)

    /** Downloads lyrics from a Genius page and stores them for [songId]. */
    suspend fun loadLyrics(songId: Long, path: String): String? =
            remoteDataSource.loadGeniusLyrics(path)?.also { localDataSource.setLyrics(Lyric(songId, it)) }

    /** Downloads lyrics when none are stored and Genius has exactly one match. Returns true if downloaded. */
    suspend fun downloadLyricsIfMissing(song: Song): Boolean {
        if (localDataSource.getLyrics(song.id) != null) return false
        val hits = remoteDataSource.searchGenius(song.title, song.artistName, exactTitle = true)
        if (hits.size != 1) return false
        return loadLyrics(song.id, hits[0].result.path) != null
    }

    suspend fun setLyrics(lyrics: Lyric) = localDataSource.setLyrics(lyrics)

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

    // Bio
    /** Artist bio and up to two tags; cached locally after the first network lookup. */
    suspend fun getBio(artistId: Long, artistName: String): Pair<String, Array<String>?>? {
        localDataSource.getBio(artistId)?.let { return it }
        val artist = remoteDataSource.searchLastFMArtist(artistName)?.artist ?: return null
        val bio = artist.bio?.summary?.takeIf { it.isNotBlank() } ?: "No Bio Found :("
        val tags = artist.tags?.tag?.take(2)?.map { it.name }?.toTypedArray()?.takeIf { it.isNotEmpty() }
        localDataSource.insertBio(artistId, bio, tags)
        return bio to tags
    }

    // Play count
    suspend fun increasePlayCount(song: Song) = localDataSource.increasePlayCount(song)

    // Metadata for the tag editor
    suspend fun searchTrackMetadata(title: String, artist: String): TrackMetadata? =
            remoteDataSource.searchTrackMetadata(title, artist)
}
