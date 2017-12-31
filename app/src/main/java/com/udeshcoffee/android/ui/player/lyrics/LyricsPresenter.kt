package com.udeshcoffee.android.ui.player.lyrics

import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.api.genius.Result
import com.udeshcoffee.android.api.genius.SearchResponse
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.remote.CallbackWrapper
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.toLastFMArtistQuery
import com.udeshcoffee.android.utils.PreferenceUtil
import io.reactivex.disposables.Disposable
import java.util.*

/**
 * Created by Udathari on 8/25/2017.
 */
class LyricsPresenter(val view: LyricsContract.View, private val dataRepository: DataRepository,
                      val sharedPreferences: SharedPreferences):
        LyricsContract.Presenter {

    object LyricsSize {
        val SMALL = 1
        val MEDIUM = 2
        val LARGE = 3
    }

    val TAG = "LyricsPresenter"
    var broadcastDisposable: Disposable? = null
    var currentSong: Song? = null
    var lyricDisposable: Disposable? = null

    init {
        view.presenter = this
    }

    override fun start() {
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            fetchLyrics()
                        }
                    }
                }
        fetchLyrics()
        view.setLyricsSize(lyricsSize)
    }

    override fun stop() {
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        disposeLyrics()
    }

    override fun fetchLyrics() {
        currentSong = getService()?.currentSong()
        currentSong?.let {
            dataRepository.getLyrics(it).subscribe({ it1 ->
                it1?.let { it2 -> view.setLyrics(it2) }
            }, { _ ->
                searchLyrics(it.id, it.title, it.artistName, true)
            })
        }
    }

    override fun changeLyricsSize() {
        when (lyricsSize) {
            LyricsPresenter.LyricsSize.SMALL -> {
                lyricsSize = LyricsPresenter.LyricsSize.MEDIUM
                view.setLyricsSize(LyricsPresenter.LyricsSize.MEDIUM)
            }
            LyricsPresenter.LyricsSize.MEDIUM -> {
                lyricsSize = LyricsPresenter.LyricsSize.LARGE
                view.setLyricsSize(LyricsPresenter.LyricsSize.LARGE)
            }
            LyricsPresenter.LyricsSize.LARGE -> {
                lyricsSize = LyricsPresenter.LyricsSize.SMALL
                view.setLyricsSize(LyricsPresenter.LyricsSize.SMALL)
            }
        }
    }

    private fun disposeLyrics() {
        lyricDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        lyricDisposable = null
    }

    private fun searchLyrics(id: Long, title: String, artist: String, shouldCheckEqual: Boolean) {
        Log.d(TAG, "Searching Lyrics id:$id title:$title artist:$artist")
        view.showSearching()
        disposeLyrics()
        lyricDisposable = dataRepository.searchLyrics(title, artist.toLastFMArtistQuery(), shouldCheckEqual)
                .subscribeWith(object : CallbackWrapper<SearchResponse>() {
            override fun onSuccess(t: SearchResponse) {
                currentSong?.id?.let {
                    if (id == it){
                        t.response?.hits?.let { it1 ->
                            if (it1.isNotEmpty()) {
                                if (it1.size == 1) {
                                    loadLyrics(it, it1[0].result.path)
                                } else {
                                    val tempList = ArrayList<Result>()
                                    it1.mapTo(tempList) { it.result }
                                    view.setMultipleFound(tempList)
                                }
                            } else {
                                view.setNotFound()
                            }
                        }
                    }
                }
            }
        })
    }

    fun loadLyrics(id: Long, path: String) {
        view.showLoading()
        if (path != "")
            dataRepository.loadLyrics(id, path)
                    .subscribe {
                        it?.let {
                            currentSong?.id?.let { it1 ->
                                if (id == it1) {
                                    view.setLyrics(it)
                                }
                            }
                        }
                    }
    }

    override fun search(id: Long, title: String, artist:String) {
        currentSong?.id?.let { if (it == id) searchLyrics(it, title, artist, false) }
    }

    override fun retry() {
        fetchLyrics()
    }

    override fun cancel() {

    }

    override fun selectFromMultiple(result: Result) {
        currentSong?.id?.let { loadLyrics(it, result.path) }
    }

    override fun openSearch() {
        currentSong?.let { view.showSearchDialog(it.id, it.title, it.artistName) }
    }

    private var lyricsSize: Int
        get() = sharedPreferences.getInt(PreferenceUtil.LYRICS_SIZE, LyricsSize.SMALL)
        set(value) {
            sharedPreferences.edit().putInt(PreferenceUtil.LYRICS_SIZE, value).apply()
        }
}