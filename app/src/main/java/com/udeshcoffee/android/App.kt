package com.udeshcoffee.android

import android.support.multidex.MultiDexApplication
import com.bumptech.glide.Glide
import org.jaudiotagger.tag.TagOptionSingleton

/**
 * Created by Udathari on 8/22/2017.
 */
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        TagOptionSingleton.getInstance().isAndroid = true

//        cleanGenres()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

//    private fun cleanGenres() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
//            return
//
//        contentResolver.toBrite()
//                .createQuery(Genre.query.uri,
//                        Genre.query.projection,
//                        Genre.query.selection,
//                        Genre.query.args,
//                        Genre.query.sort, false)
//                .mapToList{ Genre(it) }
//                .take(1)
//                .flatMapIterable{it -> it}
//                .concatMap{Observable.just(it).delay(250, TimeUnit.MILLISECONDS)}
//                .flatMap { genre ->
//                    genre.getSongCount(contentResolver)
//                            ?.doOnNext { numSongs ->
//                                if (numSongs == 0) {
//                                    try {
//                                        contentResolver.delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + " == " + genre.id, null);
//                                    } catch (e: Exception) {
//                                        //Don't care if we couldn't delete this uri.
//                                    }
//                                }
//                            }
//                }
//                // Since this is called on app launch, let's delay to allow more important tasks to complete.
//                .delaySubscription(2500, TimeUnit.MILLISECONDS)
//                .subscribe()
//    }

    companion object {

        @get: Synchronized lateinit var instance: App
    }
}
