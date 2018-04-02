package com.udeshcoffee.android.ui.player.lyrics

import com.udeshcoffee.android.data.remote.genius.Result
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
* Created by Udathari on 8/25/2017.
*/
interface LyricsContract {

    interface View: BaseView<Presenter> {

        fun showLoading()

        fun showSearching()

        fun setLyrics(lyrics: String)

        fun setLyricsSize(size: Int)

        fun setNotFound()

        fun setMultipleFound(items: List<Result>)

        fun onSearchRequest(id:Long, title: String, artist: String)

        fun onAddLyrics(id:Long, lyrics: String)

        fun setFailed()

        fun hideProgressLayout()

        fun hideErrorLayout()

        fun hideLyricLayout()

        fun hideMultipleLayout()

        fun showSearchDialog(id:Long, title:String, artist: String)

        fun showCustomLyricDialog(id:Long)
    }

    interface Presenter: BasePresenter<View> {

        fun fetchLyrics()

        fun changeLyricsSize()

        fun openSearch()

        fun openAddLyrics()

        fun search(id: Long, title: String, artist: String)

        fun addCustomLyrics(id: Long, lyrics: String)

        fun retry()

        fun cancel()

        fun selectFromMultiple(result: Result)

    }
}