package com.udeshcoffee.android.ui.player.queue

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.playback.RepeatModes
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.extensions.getPlayer
import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.call

/**
 * Created by Udathari on 9/16/2017.
 */
class QueueViewModel(
        application: Application,
        private val sharedPreferences: SharedPreferences
) : SongContainingViewModel(application) {

    val repeatMode = MutableLiveData<Int>()
    val isShuffle = MutableLiveData<Boolean>()
    var total = 0
    val playPosition = MutableLiveData<Int>()
    val showAddToPlaylist = SingleLiveEvent<List<Song>>()
    val showPlayerUI = SingleLiveEvent<Unit>()
    val hideNowPlay = SingleLiveEvent<Unit>()
    val scrollTo = SingleLiveEvent<Int>()

    private var queueJob: Job? = null
    private var loadFirstTime = true

    override fun start() {
        loadFirstTime = true
        val player = getPlayer()
        repeatMode.value = player?.repeatMode ?: sharedPreferences.getInt(PreferenceUtil.REPEAT_MODE, RepeatModes.PREF_ALL)
        isShuffle.value = player?.shuffleEnabled ?: sharedPreferences.getBoolean(PreferenceUtil.SHUFFLE, false)
        super.start()
    }

    override fun fetchSongs() {
        disposeSongs()
        val player = getPlayer() ?: return
        queueJob = viewModelScope.launch {
            player.queueChanges.collect { queue ->
                songs.value = queue
                if (loadFirstTime) {
                    scrollTo.value = player.playPosition
                    loadFirstTime = false
                }
            }
        }
        total = player.queue.size
        playPosition.value = player.playPosition
    }

    override fun onMetaDataChanged(service: PlayerConnection) {
        super.onMetaDataChanged(service)
        total = service.queue.size
        playPosition.value = service.playPosition
    }

    override fun disposeSongs() {
        queueJob?.cancel()
    }

    fun closeQueue() {
        showPlayerUI.call()
    }

    fun addToPlaylist() {
        getPlayer()?.let { showAddToPlaylist.value = it.queue }
    }

    fun changeRepeatMode() {
        val next = when (repeatMode.value) {
            RepeatModes.PREF_NONE -> RepeatModes.PREF_ALL
            RepeatModes.PREF_ALL -> RepeatModes.PREF_ONE
            else -> RepeatModes.PREF_NONE
        }
        // The service persists the mode.
        getPlayer()?.repeatMode = next
        repeatMode.value = next
    }

    fun shuffle() {
        val enabled = !(isShuffle.value ?: false)
        getPlayer()?.shuffleEnabled = enabled
        isShuffle.value = enabled
    }

    /** The player keeps track of the current song's index when items move. */
    fun itemMoved(fromPosition: Int, toPosition: Int) {
        getPlayer()?.moveInQueue(fromPosition, toPosition)
    }

    fun queueItemClicked(position: Int) {
        getPlayer()?.playAt(position)
    }

    fun itemRemoved(position: Int) {
        val player = getPlayer() ?: return
        if (player.queue.size > 1) player.removeFromQueue(position) else clearQueue()
    }

    fun clearQueue() {
        getPlayer()?.clearQueue()
        hideNowPlay.call()
        closeQueue()
    }

    // Redundant
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false

    companion object {
        private const val TAG = "QueuePresenter"
    }

}