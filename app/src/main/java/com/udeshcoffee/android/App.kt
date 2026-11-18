package com.udeshcoffee.android

import android.os.Build
import android.app.Application
import com.bumptech.glide.Glide
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.di.appModule
import com.udeshcoffee.android.utils.Permissions
import org.jaudiotagger.tag.TagOptionSingleton
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
* Created by Udathari on 8/22/2017.
*/
class App : Application() {

    val mediaRepository by inject<MediaRepository>()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        instance = this

        TagOptionSingleton.getInstance().isAndroid = true

        cleanGenres()
    }

    override fun onLowMemory() {

        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    private fun cleanGenres() {
        // MediaStore genres are read-only for apps from API 29.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !Permissions.hasReadAudio(this))
            return

        mediaRepository.cleanGenres()
    }

    companion object {

        @get: Synchronized lateinit var instance: App
    }
}
