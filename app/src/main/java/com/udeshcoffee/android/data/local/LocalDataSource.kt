package com.udeshcoffee.android.data.local

import androidx.room.withTransaction
import com.udeshcoffee.android.data.model.*
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.flow.Flow

/**
* App-owned data in the Room database.
*/
class LocalDataSource(
        private val database: LocalDatabase,
        private val dataDao: DataDao,
        val playlistDao: PlaylistDao
) {

    // Stats
    fun observeMostPlayedSongs(): Flow<List<SongStat>> = dataDao.observeSongMostPlayed()

    fun observeRecentlyPlayedSongs(): Flow<List<SongStat>> = dataDao.observeSongRecentlyPlayed()

    suspend fun increasePlayCount(song: Song) {
        val now = System.currentTimeMillis() / 1000
        database.withTransaction {
            val songStat = dataDao.getSongStat(song.id)
            dataDao.setStats(songStat?.apply { playCount++; playTime = now } ?: SongStat(song.id, 1, now))
            val albumStat = dataDao.getAlbumStat(song.albumId)
            dataDao.setStats(albumStat?.apply { playCount++; playTime = now } ?: AlbumStat(song.albumId, 1, now))
            val artistStat = dataDao.getArtistStat(song.artistId)
            dataDao.setStats(artistStat?.apply { playCount++; playTime = now } ?: ArtistStat(song.artistId, 1, now))
        }
    }

    // Lyric
    suspend fun getLyrics(songId: Long): String? = dataDao.getLyrics(songId)?.lyric

    suspend fun setLyrics(lyrics: Lyric) = dataDao.setLyrics(lyrics)

    // Favorites
    suspend fun isFavorite(songId: Long): Boolean = dataDao.getFavorite(songId) != null

    fun observeIsFavorite(songId: Long): Flow<Boolean> = dataDao.observeIsFavorite(songId)

    suspend fun addFavorite(favorite: Favorite) = dataDao.addFavorite(favorite)

    suspend fun addFavorites(songIds: List<Long>) = dataDao.addFavorites(songIds.map { Favorite(it) })

    suspend fun deleteFavorite(favorite: Favorite) = dataDao.deleteFavorite(favorite)

    fun observeFavorites(): Flow<List<Favorite>> = dataDao.observeFavorites()

    // Bio
    suspend fun getBio(artistId: Long): Pair<String, Array<String>?>? =
            dataDao.getBio(artistId)?.let { bio ->
                val text = bio.bio ?: return null
                text to bio.tags?.split(",")?.filter { it.isNotBlank() }?.toTypedArray()
            }

    suspend fun insertBio(artistId: Long, bio: String, tags: Array<String>?) {
        dataDao.insertBio(Bio(artistId, bio, tags?.joinToString(",") ?: ""))
    }
}
