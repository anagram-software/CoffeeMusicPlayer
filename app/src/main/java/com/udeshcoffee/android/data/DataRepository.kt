package com.udeshcoffee.android.data

import android.util.Log
import com.udeshcoffee.android.data.remote.itunes.SearchResponse
import com.udeshcoffee.android.data.remote.lastfm.ArtistResponse
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.model.EQPreset
import com.udeshcoffee.android.data.model.Favorite
import com.udeshcoffee.android.data.model.Lyric
import com.udeshcoffee.android.data.remote.RemoteDataSource
import com.udeshcoffee.android.data.remote.itunes.ItunesSong
import com.udeshcoffee.android.model.Song
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 12/19/2017.
 */
class DataRepository(
        private val localDataSource: LocalDataSource,
        private val remoteDataSource: RemoteDataSource
) {

    private val NULL_ID: Long = -256

    // Lyric
    fun getLyrics(song: Song): Observable<String?> {
        return Observable.fromCallable { localDataSource.getLyrics(song.id) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
    }

    fun searchLyrics(title: String, artist: String, shouldCheckEqual: Boolean): Single<com.udeshcoffee.android.data.remote.genius.SearchResponse> =
            remoteDataSource.searchGenius(title, artist, shouldCheckEqual)
                    .singleOrError()

    fun loadLyrics(songId: Long, path: String): Single<String?> {
        return remoteDataSource.loadGenius(path)
                .doOnNext { it?.let { localDataSource.setLyrics(Lyric(songId, it)) } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError()
    }

    fun downloadLyricsIfMissing(song: Song): Boolean {
        val lyrics = localDataSource.getLyrics(song.id)
        if (lyrics == null) {
            remoteDataSource.searchGeniusSynchronous(song.title, song.artistName)?.response?.hits?.let { it1 ->
                if (it1.isNotEmpty()) {
                    if (it1.size == 1) {
                        setLyrics(Lyric(song.id, remoteDataSource.loadGeniusSynchronous(it1[0].result.path)))
                        return true
                    }
                }
            }
            return false
        }
        return false
    }

    fun setLyrics(lyrics: Lyric) = localDataSource.setLyrics(lyrics)

    // Equalizer
    fun getEQPresets(): List<EQPreset> = localDataSource.getEQPresets()

    fun addEQPreset(eqPreset: EQPreset): Long = localDataSource.addEQPreset(eqPreset)

    fun deleteEQPreset(eqPreset: EQPreset) = localDataSource.deleteEQPreset(eqPreset)

    // Favorites
    fun isFavorite(songId: Long): Single<Boolean> {
        return Observable.fromCallable { localDataSource.isFavorite(songId) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .first(false)
    }

    fun isFavoriteSync(songId: Long): Boolean = localDataSource.isFavorite(songId)

    fun toggleFavorite(songId: Long, callback: ((isFavorite: Boolean) -> Unit)? = null) {
        Log.d("toggleFavorite", "toggleFavorite")
        Observable.fromCallable { localDataSource.isFavorite(songId) }
                .map {
                    Log.d("toggleFavorite", "doOnSuccess $it")
                    if (it) {
                        deleteFavorite(Favorite(songId))
                        Log.d("toggleFavorite", "delete")
                        false
                    } else {
                        addFavorite(Favorite(songId))
                        Log.d("toggleFavorite", "add")
                        true
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback?.let { it1 -> it1(it) }
                }, {
                    Log.d("toggleFavorite", "error $it")
                })
    }

    fun addToFavorites(songs: List<Song>) {
        Observable.just(songs)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .take(1)
                .subscribe {
                    it.indices
                            .filterNot { localDataSource.isFavorite(songs[it].id) }
                            .forEach { addFavorite(Favorite(songs[it].id)) }
                }
    }

    fun addFavorite(favorite: Favorite) = localDataSource.addFavorite(favorite)

    private fun deleteFavorite(favorite: Favorite) = localDataSource.deleteFavorite(favorite)

    // Bio
    fun getBio(artistId: Long, artistName: String): Single<Pair<String, Array<String>?>> {
        return localDataSource.getBio(artistId)
                .onErrorResumeNext {
                    return@onErrorResumeNext remoteDataSource.searchLastFMArtist(artistName)
                            .map {
                                val bio = it.artist.bio?.summary ?: "No Bio Found :("
                                val tags = it.artist.tags.tag
                                val tagsArray = if (tags.isNotEmpty()) {
                                    Array(2.coerceAtMost(tags.size - 1)) { it1 ->
                                        tags[it1].name
                                    }
                                } else {
                                    null
                                }
                                return@map Pair(bio, tagsArray)
                            }
                            .doAfterSuccess {
                                localDataSource.insertBio(artistId, it.first, it.second)
                            }

                }
    }

    // PlayCount
    fun increasePlayCount(song: Song) {
        localDataSource.increasePlayCount(song)
    }

    // Metadata
    fun searchItunes(title: String, artist: String): Observable<List<ItunesSong>> =
            remoteDataSource.searchItunes(title, artist)

}