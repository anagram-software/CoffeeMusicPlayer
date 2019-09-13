package com.udeshcoffee.android.data.local

import androidx.room.*
import com.udeshcoffee.android.data.model.*
import io.reactivex.Flowable
import io.reactivex.Single

/**
* Created by Udathari on 12/19/2017.
*/
@Dao
interface DataDao {

    // Song Stats
    @Query("SELECT * FROM song_stats WHERE songid = :songId")
    fun getSongStat(songId: Long): Single<SongStat>

    @Query("SELECT * FROM song_stats ORDER BY playcount DESC")
    fun getSongMostPlayed(): List<SongStat>

    @Query("SELECT * FROM song_stats ORDER BY playtime ASC")
    fun getSongRecentlyPlayed(): List<SongStat>

    @Query("SELECT * FROM song_stats ORDER BY playcount DESC LIMIT :limit")
    fun getTopSongs(limit: Int): List<SongStat>

    // Album Stats
    @Query("SELECT * FROM album_stats WHERE albumid = :albumId")
    fun getAlbumStat(albumId: Long): Single<AlbumStat>

    @Query("SELECT * FROM album_stats ORDER BY playcount DESC LIMIT :limit")
    fun getTopAlbums(limit: Int): List<AlbumStat>

    // Artists Stats
    @Query("SELECT * FROM artist_stats WHERE artistid = :artistId")
    fun getArtistStat(artistId: Long): Single<ArtistStat>

    @Query("SELECT * FROM artist_stats ORDER BY playcount DESC LIMIT :limit")
    fun getTopArtists(limit: Int): List<ArtistStat>

    // Stats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setStats(songStat: SongStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setStats(albumStats: AlbumStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setStats(artistStats: ArtistStat)

    // Lyrics
    @Query("SELECT * FROM lyrics WHERE songid = :songId")
    fun getLyrics(songId: Long): Lyric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setLyrics(lyrics: Lyric)

    // Equalizer
    @Insert
    fun addEQPreset(eqPreset: EQPreset): Long

    @Delete
    fun deleteEQPreset(eqPreset: EQPreset)

    @Query("SELECT * FROM equalizer")
    fun getEQPresets(): List<EQPreset>

    // Favorites
    @Query("SELECT * FROM favorites WHERE fav = :songId")
    fun getFavorite(songId: Long): Favorite?

    @Query("SELECT * FROM favorites")
    fun getFavorites(): Flowable<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFavorite(favorite: Favorite)

    @Delete
    fun deleteFavorite(favorite: Favorite)

    // Bio
    @Query("SELECT * FROM bio WHERE artistid = :artistId")
    fun getBio(artistId: Long): Single<Bio>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertBio(bio: Bio)

}