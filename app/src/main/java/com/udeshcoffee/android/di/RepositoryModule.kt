package com.udeshcoffee.android.di

import android.arch.persistence.room.Room
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.local.LocalDatabase
import com.udeshcoffee.android.data.remote.RemoteDataSource
import org.koin.dsl.module.applicationContext

/**
 * Created by Udathari on 1/14/2018.
 */
val repositoryModule = applicationContext {

    factory { Room.databaseBuilder(get(), LocalDatabase::class.java, "CoffeePlayer.db")
            .addMigrations(LocalDatabase.Migration_5_6)
            .allowMainThreadQueries()
            .build().dataDao() }

    factory { RemoteDataSource }

    bean { LocalDataSource(get()) }

    bean { DataRepository(get(), get()) }

    bean { MediaRepository(get(), get()) }

}