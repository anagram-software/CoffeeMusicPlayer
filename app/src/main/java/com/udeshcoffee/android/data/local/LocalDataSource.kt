package com.udeshcoffee.android.data.local

import android.support.annotation.VisibleForTesting
import android.util.Log
import com.udeshcoffee.android.data.model.*
import com.udeshcoffee.android.model.Song
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 12/19/2017.
 */
class LocalDataSource(private val dataDao: DataDao) {

    

    fun getMostPlayedSongs(): Observable<List<SongStat>> =
            Observable.fromCallable { dataDao.getSongMostPlayed() }

    fun getRecentlyPlayedSongs(): Observable<List<SongStat>> =
            Observable.fromCallable { dataDao.getSongRecentlyPlayed() }

    fun getTopSongs(): Observable<List<SongStat>> =
            Observable.fromCallable { dataDao.getTopSongs(5) }

    fun getTopAlbums(): Observable<List<AlbumStat>> =
            Observable.fromCallable { dataDao.getTopAlbums(5) }

    fun getTopArtists(): Observable<List<ArtistStat>> =
            Observable.fromCallable { dataDao.getTopArtists(5) }

    // Lyric
    fun getLyrics(songId: Long): String? = dataDao.getLyrics(songId)?.lyric

    fun setLyrics(lyrics: Lyric) = dataDao.setLyrics(lyrics)

    // Equalizer
    fun getEQPresets(): List<EQPreset> = dataDao.getEQPresets()

    fun addEQPreset(eqPreset: EQPreset): Long = dataDao.addEQPreset(eqPreset)

    fun deleteEQPreset(eqPreset: EQPreset) = dataDao.deleteEQPreset(eqPreset)

    // Favorites
    fun isFavorite(songId: Long): Boolean{
        val favorite = dataDao.getFavorite(songId)
        return favorite != null
    }

    fun addFavorite(favorite: Favorite) = dataDao.addFavorite(favorite)

    fun deleteFavorite(favorite: Favorite) = dataDao.deleteFavorite(favorite)

    fun getFavorites(): Observable<List<Favorite>> =
            Observable.fromCallable { dataDao.getFavorites() }

    // Bio
    fun getBio(artistId: Long): Single<Pair<String, Array<String>?>> =
            dataDao.getBio(artistId).map {
                Pair(it.bio!!, it.tags!!.split(",").toTypedArray())
            }

    fun insertBio(artistId: Long, bio: String, tags: Array<String>?) {
        var tagsString = ""
        tags?.forEach {
            tagsString += if (tagsString == "")
                it
            else
                "," + it
        }
        dataDao.insertBio(Bio(artistId, bio, tagsString))
    }

    // PlayCount
    fun increasePlayCount(song: Song) {
        dataDao.getSongStat(song.id)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe ({
                    Log.d("playCount", "increasePlayCount ${it.playCount}")
                    it.playCount++
                    Log.d("playCount", "increasePlayCount ++ ${it.playCount}")
                    it.playTime = System.currentTimeMillis() / 1000
                    Log.d("playCount", "increasePlayCount")
                    dataDao.setStats(it)
                }, { dataDao.setStats(SongStat(song.id, 1, System.currentTimeMillis() / 1000)) })
        dataDao.getAlbumStat(song.albumId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe ({
                    it.playCount++
                    it.playTime = System.currentTimeMillis() / 1000
                    dataDao.setStats(it)
                }, { dataDao.setStats(AlbumStat(song.albumId, 1, System.currentTimeMillis() / 1000)) })
        dataDao.getArtistStat(song.artistId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe ({
                    it.playCount++
                    it.playTime = System.currentTimeMillis() / 1000
                    dataDao.setStats(it)
                }, { dataDao.setStats(ArtistStat(song.artistId, 1, System.currentTimeMillis() / 1000)) })
    }

    companion object {
        private var INSTANCE: LocalDataSource? = null

        @JvmStatic
        fun getInstance(dataDao: DataDao): LocalDataSource {
            if (INSTANCE == null) {
                synchronized(LocalDataSource::javaClass) {
                    INSTANCE = LocalDataSource(dataDao)
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }

}