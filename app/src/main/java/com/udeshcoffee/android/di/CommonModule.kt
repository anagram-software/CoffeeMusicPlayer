package com.udeshcoffee.android.di

import androidx.preference.PreferenceManager
import org.koin.dsl.module

val commonModule = module {
    factory { PreferenceManager.getDefaultSharedPreferences(get()) }
}
