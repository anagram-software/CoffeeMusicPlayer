package com.udeshcoffee.android

import android.app.Application
import android.os.StrictMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.AlbumArtFetcher
import com.udeshcoffee.android.utils.CoverArtCache
import com.udeshcoffee.android.utils.CoverArtMapper
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import org.koin.core.context.GlobalContext
import com.udeshcoffee.android.di.appModule
import org.jaudiotagger.tag.TagOptionSingleton
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        instance = this

        TagOptionSingleton.getInstance().isAndroid = true
    }

    /** Debug builds log disk/network access on the main thread and leaked resources. */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectContentUriWithoutPermission()
                .penaltyLog()
                .build())
    }

    /** Coil's app-wide loader, with MediaStore album art support (see [AlbumArt]). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
            ImageLoader.Builder(context)
                    .components {
                        add(AlbumArtFetcher.Factory())
                        add(AlbumArtFetcher.Key())
                        // Server artwork: mapped to an authenticated URL, cached under the model (not the URL with its token).
                        // (A Keyer<CoverArt> wouldn't run: keyers see the mapped URL.)
                        add(CoverArtCache.Keys())
                        add(CoverArtMapper { GlobalContext.get().get<SubsonicRepository>() })
                    }
                    .eventListener(CoverArtCache.KeepLatest())
                    .build()

    companion object {

        @get: Synchronized lateinit var instance: App
    }
}
