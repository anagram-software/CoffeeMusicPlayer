package com.udeshcoffee.android.ui.main.detail.albumdetail

import com.udeshcoffee.android.ui.main.ShufflableSongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
* Created by Udathari on 9/12/2017.
*/
interface AlbumDetailContract {

    interface View: SongContainingView<Presenter>

    interface Presenter: ShufflableSongContainingPresenter<View> {

        fun fetchData()

        var albumId: Long

        var sortOrder: Int

        var sortAscending: Boolean

    }

}