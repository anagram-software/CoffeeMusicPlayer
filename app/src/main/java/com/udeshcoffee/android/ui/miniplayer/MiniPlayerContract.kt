package com.udeshcoffee.android.ui.miniplayer

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
 * Created by Udathari on 8/25/2017.
 */
interface MiniPlayerContract {

    interface View: BaseView<Presenter> {

        fun setDetails(song: Song)

        fun initProgress(max: Long)

        fun setProgress(duration: Long)

        fun setPlayOrPause(isPlay: Boolean)

        fun setAlpha(alpha: Float)

        fun setVisibility(visibility: Int)

        fun showNowPlayUI()

    }

    interface Presenter: BasePresenter {

        fun loadDetails()

        fun setPlaying()

        fun playPauseToggle()

        fun gotoNext()

        fun openNowPlay()

        fun setAlpha(alpha: Float)

        fun setVisibility(visibility: Int)

        fun setProgress()

        var isPlaying: Boolean
    }
}