package com.udeshcoffee.android.ui.player.queue

import android.content.IntentFilter
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent

/**
 * Created by Udathari on 9/16/2017.
 */
class QueuePresenter(): QueueContract.Presenter, KoinComponent {

    val TAG = "QueuePresenter"

    override lateinit var view: QueueContract.View

    private var queueDisposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null
    private var loadFirstTime = true

    override fun start() {
        loadFirstTime = true
        loadQueue()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            getService()?.let { it1 ->
                                view.setCurrentSong(it1.playPosition, true)
                            }
                        }
                    }
                }
    }

    override fun stop() {
        queueDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun loadQueue() {
        Log.d(TAG, "loadQueue")
        getService()?.let {
            queueDisposable = it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe { it1 ->
                        Log.d(TAG, "loadQueue: ${it1.size}")
                        view.showQueue(it1)
                        view.setCurrentSong(it.playPosition, false)
                        if (loadFirstTime) {
                            view.scrollTo(it.playPosition)
                            loadFirstTime = false
                        }
                    }
        }
    }

    override fun closeQueue() {
        view.showPlayerUI()
    }

    override fun addToPlaylist() {
        getService()?.let {
            it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .take(1)
                    .subscribe { it1 ->
                        view.showAddToPlaylistUI(it1)
                    }
        }
    }

    override fun itemClicked(position: Int) {
        getService()?.initSong(position, true)
    }

    override fun itemMoved(fromPosition: Int, toPosition: Int) {
        val service = getService()
        service?.playPosition?.let { currentPos ->
            when {
                (currentPos in (fromPosition + 1)..toPosition) -> setCurrentPosition(currentPos - 1)
                (currentPos in toPosition..(fromPosition - 1)) -> setCurrentPosition(currentPos + 1)
                fromPosition == currentPos -> setCurrentPosition(toPosition)
                else -> setCurrentPosition(currentPos)
            }
        }
    }

    fun setCurrentPosition(position: Int) {
        getService()?.playPosition = position
        view.setCurrentSong(position, false)
    }

    override fun itemRemoved(position: Int) {
        getService()?.let {
            it.list.remove(position)
            if (it.list.size >= 1) {
                val currentPos =  it.playPosition
                if (position == currentPos) {
                    it.initSong(position, true)
                } else if (position < it.playPosition) {
                    it.playPosition = currentPos - 1
                }
            } else {
                clearQueue()
            }
        }
    }

    override fun clearQueue() {
        getService()?.clearSongList()
        view.hideOnClearQueue()
        closeQueue()
    }

}