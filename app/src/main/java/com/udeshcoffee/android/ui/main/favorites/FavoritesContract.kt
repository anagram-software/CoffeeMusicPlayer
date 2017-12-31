package com.udeshcoffee.android.ui.main.favorites

import com.udeshcoffee.android.ui.main.ShufflableSongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
 * Created by Udathari on 9/29/2017.
 */
interface FavoritesContract {

    interface View : SongContainingView<Presenter> {

    }

    interface Presenter : ShufflableSongContainingPresenter {

        fun fetchData()

        var sortOrder: Int

        var sortAscending: Boolean

    }

}