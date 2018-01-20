package com.udeshcoffee.android.ui.main.library.nested.track

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
* Created by Udathari on 9/5/2017.
*/
interface TrackContract {

    interface View: SongContainingView<Presenter>

    interface Presenter: BasePresenter<View> {

        fun fetchData()

        fun itemClicked(position: Int, allItems: List<Song>)

        fun itemLongClicked(item: Song)

        fun shuffleClicked(allItems: List<Song>)

        var sortOrder: Int

        var sortAscending: Boolean

    }

}