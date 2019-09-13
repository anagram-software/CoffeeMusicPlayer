package com.udeshcoffee.android.di

import androidx.room.Room
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.local.LocalDatabase
import com.udeshcoffee.android.data.remote.RemoteDataSource
import org.koin.dsl.module.module

/**
* Created by Udathari on 1/14/2018.
*/
val repositoryModule = module {

    factory { Room.databaseBuilder(get(), LocalDatabase::class.java, "CoffeePlayer.db")
            .addMigrations(LocalDatabase.Migration_5_6)
            .allowMainThreadQueries()
            .build().dataDao() }

    factory { RemoteDataSource }

    single { LocalDataSource(get()) }

    single { DataRepository(get(), get()) }

    single { MediaRepository(get(), get()) }

}