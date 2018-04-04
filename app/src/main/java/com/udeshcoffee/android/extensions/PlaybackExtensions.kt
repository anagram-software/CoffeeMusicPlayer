package com.udeshcoffee.android.extensions

import android.arch.lifecycle.AndroidViewModel
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.utils.ServiceConnectionUtil
import java.util.*

/**
* Created by Udathari on 9/17/2017.
*/
// Init Song
private fun playSongLogic(position: Int, allItems: List<Song>, shouldPlay: Boolean) {
    if (allItems.isNotEmpty()) {
        val service = ServiceConnectionUtil.binder?.service?.get()

        service?.setSongList(allItems as ArrayList<Song>)
        service?.initSong(position, shouldPlay)
        service?.checkAndShuffle()
    }
}

fun <T> BasePresenter<T>.playSong(position: Int, allItems: List<Song>, shouldPlay: Boolean) {
    if (allItems.isNotEmpty()) {
        playSongLogic(position, allItems, shouldPlay)
    }
}

fun Fragment.playSong(position: Int, allItems: List<Song>, shouldPlay: Boolean) {
    if (allItems.isNotEmpty()) {
        playSongLogic(position, allItems, shouldPlay)
    }
}

fun AndroidViewModel.playSong(position: Int, allItems: List<Song>, shouldPlay: Boolean) {
    if (allItems.isNotEmpty()) {
        playSongLogic(position, allItems, shouldPlay)
    }
}

// Queue Song
private fun queueSongLogic(allItems: List<Song>, isNext: Boolean) {
    if (allItems.isNotEmpty()) {
        val service = ServiceConnectionUtil.binder?.service?.get()

        service?.apply {
            val shouldPlay = list.isEmpty()
            if (isNext) {
                list.addAll(playPosition + 1, allItems)
            } else {
                list.addAll(allItems)
            }

            checkAndAddToUnshuffledList(allItems as ArrayList<Song>)

            if (shouldPlay) {
                initSong(0, true)
                checkAndShuffle()
            }
        }
    }
}

fun <T> BasePresenter<T>.queueSong(allItems: List<Song>, isNext: Boolean) {
    if (allItems.isNotEmpty()) {
        queueSongLogic(allItems, isNext)
    }
}

fun DialogFragment.queueSong(allItems: List<Song>, isNext: Boolean) {
    if (allItems.isNotEmpty()) {
        queueSongLogic(allItems, isNext)
    }
}

fun AndroidViewModel.queueSong(allItems: List<Song>, isNext: Boolean) {
    if (allItems.isNotEmpty()) {
        queueSongLogic(allItems, isNext)
    }
}

// Shuffle and play
fun <T> BasePresenter<T>.shuffle(allItems: List<Song>) {
    if (allItems.isNotEmpty()) {
        val tempList = allItems.toMutableList() as ArrayList<Song>
        if (tempList.size > 1)
            tempList.shuffle()
        playSongLogic(0, tempList, true)
    }
}

fun AndroidViewModel.shuffle(allItems: List<Song>) {
    if (allItems.isNotEmpty()) {
        val tempList = allItems.toMutableList() as ArrayList<Song>
        if (tempList.size > 1)
            tempList.shuffle()
        playSongLogic(0, tempList, true)
    }
}