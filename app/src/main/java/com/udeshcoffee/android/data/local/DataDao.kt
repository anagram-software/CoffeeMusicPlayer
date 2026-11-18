package com.udeshcoffee.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.udeshcoffee.android.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DataDao {

    // Stats
    @Query("SELECT * FROM song_stats WHERE songid = :songId")
    suspend fun getSongStat(songId: Long): SongStat?

    @Query("SELECT * FROM song_stats ORDER BY playcount DESC")
    fun observeSongMostPlayed(): Flow<List<SongStat>>

    @Query("SELECT * FROM song_stats ORDER BY playtime DESC")
    fun observeSongRecentlyPlayed(): Flow<List<SongStat>>

    @Query("SELECT * FROM album_stats WHERE albumid = :albumId")
    suspend fun getAlbumStat(albumId: Long): AlbumStat?

    @Query("SELECT * FROM artist_stats WHERE artistid = :artistId")
    suspend fun getArtistStat(artistId: Long): ArtistStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setStats(songStat: SongStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setStats(albumStats: AlbumStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setStats(artistStats: ArtistStat)

    // Lyrics
    @Query("SELECT * FROM lyrics WHERE songid = :songId")
    suspend fun getLyrics(songId: Long): Lyric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLyrics(lyrics: Lyric)

    // Equalizer
    @Insert
    suspend fun addEQPreset(eqPreset: EQPreset): Long

    @Delete
    suspend fun deleteEQPreset(eqPreset: EQPreset)

    @Query("SELECT * FROM equalizer")
    suspend fun getEQPresets(): List<EQPreset>

    // Favorites
    @Query("SELECT * FROM favorites WHERE fav = :songId")
    suspend fun getFavorite(songId: Long): Favorite?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE fav = :songId)")
    fun observeIsFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT * FROM favorites")
    fun observeFavorites(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(favorites: List<Favorite>)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    // Bio
    @Query("SELECT * FROM bio WHERE artistid = :artistId")
    suspend fun getBio(artistId: Long): Bio?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBio(bio: Bio)
}
