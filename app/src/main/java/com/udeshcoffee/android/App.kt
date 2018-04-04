package com.udeshcoffee.android

import android.content.pm.PackageManager
import android.support.multidex.MultiDexApplication
import android.support.v4.content.ContextCompat
import com.bumptech.glide.Glide
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.di.appModule
import org.jaudiotagger.tag.TagOptionSingleton
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin

/**
* Created by Udathari on 8/22/2017.
*/
class App : MultiDexApplication() {

    val mediaRepository by inject<MediaRepository>()

    override fun onCreate() {
        super.onCreate()
        startKoin(this, appModule)
        instance = this

        TagOptionSingleton.getInstance().isAndroid = true

        cleanGenres()
    }

    override fun onLowMemory() {

        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    private fun cleanGenres() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return

        mediaRepository.cleanGenres()
    }

    companion object {

        @get: Synchronized lateinit var instance: App
    }
}
