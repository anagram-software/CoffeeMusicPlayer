package com.udeshcoffee.android.ui.player.player

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView


/**
* Created by Udathari on 8/25/2017.
*/
interface PlayerContract {

    interface View: BaseView<Presenter> {
        fun setDetails(song: Song, animate: Boolean)

        fun setPlayerArtPager(list: List<Song>)

        fun setPlayPosition(playPosition: Int, smoothScroll: Boolean)

        fun initProgress(max: Long)

        fun setProgress(duration: Long)

        fun setCurrent(duration: Long)

        fun showHideSeekTime(isShow: Boolean)

        fun setSeekTime(duration: Long)

        fun setQueueSize(size: String)

        fun setPlayOrPause(isPlay: Boolean)

        fun setFav(isFav: Boolean)

        fun setRepeatMode(mode: Int)

        fun setShuffle(isShuffled: Boolean)

        fun showLyricUI()

        fun hideLyricUI()

        fun showMoreDialog(song: Song)

        fun showMainUI()

        fun showQueueUI()
    }

    interface Presenter: BasePresenter<View> {

        fun artScrolled(position: Int)

        fun showMore()

        fun playPauseToggle()

        fun startSeek()

        fun seeking(progress: Long)

        fun seeked(progress: Long)

        fun gotoNext()

        fun gotoBack()

        fun changeRepeatMode()

        fun shuffle()

        fun favToggle()

        fun lyricsToggle()

        fun closeNowPlay()

        fun openQueue()

        var isPlaying: Boolean

        var isSeeking: Boolean

        var repeatMode: Int

        var isShuffle: Boolean

        var isLyricsShowing: Boolean

    }
}