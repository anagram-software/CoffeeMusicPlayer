package com.udeshcoffee.android.di

import androidx.room.Room
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.local.LocalDatabase
import com.udeshcoffee.android.data.remote.RemoteDataSource
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.QueueStore
import org.koin.dsl.module

/**
* Created by Udathari on 1/14/2018.
*/
val repositoryModule = module {

    // One database instance for the whole app (previously a new Room instance was built per injection).
    single {
        Room.databaseBuilder(get(), LocalDatabase::class.java, "CoffeePlayer.db")
            .addMigrations(LocalDatabase.Migration_5_6, LocalDatabase.Migration_6_7)
            .allowMainThreadQueries()
            .build()
    }

    factory { get<LocalDatabase>().dataDao() }

    factory { get<LocalDatabase>().playlistDao() }

    factory { RemoteDataSource }

    single { LocalDataSource(get(), get()) }

    single { DataRepository(get(), get()) }

    single { MediaRepository(get(), get()) }

    single { QueueStore(get(), get()) }

    single { PlayerConnection(get(), get(), get()) }

}