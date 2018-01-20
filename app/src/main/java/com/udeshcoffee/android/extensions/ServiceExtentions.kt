package com.udeshcoffee.android.extensions

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.annimon.stream.Stream
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.utils.DopeUtil
import com.udeshcoffee.android.utils.PreferenceUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 8/26/2017.
 */

val TAG = "ServiceExtensions"

fun AppWidgetProvider.getQueue(context: Context, mediaRepository: MediaRepository): Observable<ArrayList<Song>>? {
    return getQueueLogic(context, mediaRepository)
}

fun MusicService.getQueue(): Observable<ArrayList<Song>>? {
    return getQueueLogic(this, mediaRepository)
}

private fun getQueueLogic(context: Context, mediaRepository: MediaRepository): Observable<ArrayList<Song>>? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return null
    }
    try {
        val q = PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceUtil.LAST_QUEUE, "")

        val ids = DopeUtil.reserseHexa(q)

        return mediaRepository.getSongs()
                .map { songs: List<Song> ->
                    val temp = ArrayList<Song>()
                    Stream.of(ids)
                            .forEach { id ->
                                Stream.of(songs)
                                        .forEach { song ->
                                            if (id == song.id) {
                                                temp.add(song)
                                            }
                                        }
                            }
                    return@map temp
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .take(1)
    } catch (ignored: Exception) {
        return null
    }
}

fun MusicService.getSharedData(what: String): Int {
    return this.sharedPreferences.getInt(what, 0)
}

fun MusicService.saveQueue(songs: ArrayList<Song>, lastPlayedPos: Int, lastCurrentPos: Long) {
    val editor = this.sharedPreferences.edit()

    val lastQueue = DopeUtil.hexa(songs)
    Log.d(TAG, "saving: $lastQueue")

    editor.putString(PreferenceUtil.LAST_QUEUE, lastQueue)
            .putInt(PreferenceUtil.LAST_PLAYED_POS, lastPlayedPos)
            .putInt(PreferenceUtil.LAST_CURRENT_POS, lastCurrentPos.toInt())
            .apply()
}