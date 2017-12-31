package com.udeshcoffee.android.service

import android.os.Binder
import java.lang.ref.WeakReference

/**
 * Created by Udesh Kumarasinghe on 8/22/2017.
 */
class MusicBinder(musicService: MusicService) : Binder() {

    var service : WeakReference<MusicService> = WeakReference(musicService)

}