package com.udeshcoffee.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.AlbumArtFetcher
import com.udeshcoffee.android.di.appModule
import org.jaudiotagger.tag.TagOptionSingleton
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
* Created by Udathari on 8/22/2017.
*/
class App : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        instance = this

        TagOptionSingleton.getInstance().isAndroid = true

    }

    /** Coil's app-wide loader, with MediaStore album art support (see [AlbumArt]). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
            ImageLoader.Builder(context)
                    .components {
                        add(AlbumArtFetcher.Factory())
                        add(AlbumArtFetcher.Key())
                    }
                    .build()

    companion object {

        @get: Synchronized lateinit var instance: App
    }
}
