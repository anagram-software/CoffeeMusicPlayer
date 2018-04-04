package com.udeshcoffee.android.di

import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val viewModelModule = applicationContext {
    viewModel{ AlbumDetailViewModel(get(), get(), get()) }
}