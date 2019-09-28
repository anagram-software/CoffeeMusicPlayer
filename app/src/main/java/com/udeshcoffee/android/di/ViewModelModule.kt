package com.udeshcoffee.android.di

import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailViewModel
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailViewModel
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailViewModel
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailViewModel
import com.udeshcoffee.android.ui.main.editor.EditorViewModel
import com.udeshcoffee.android.ui.main.favorites.FavoritesViewModel
import com.udeshcoffee.android.ui.main.library.nested.folder.FolderViewModel
import com.udeshcoffee.android.ui.main.library.nested.track.TrackViewModel
import com.udeshcoffee.android.ui.main.search.SearchViewModel
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerViewModel
import com.udeshcoffee.android.ui.player.player.PlayerViewModel
import com.udeshcoffee.android.ui.player.queue.QueueViewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val viewModelModule = module {
    // Detail
    viewModel{ AlbumDetailViewModel(get(), get(), get()) }
    viewModel{ ArtistDetailViewModel(get(), get(), get()) }
    viewModel{ GenreDetailViewModel(get(), get(), get()) }
    viewModel{ PlaylistDetailViewModel(get(), get(), get()) }

    // Library
    viewModel { TrackViewModel(get(), get()) }
    viewModel { FolderViewModel(get(), get(), get()) }

    // Features
    viewModel{ FavoritesViewModel(get(), get(), get()) }
    viewModel{ SearchViewModel(get(), get(), get()) }
    viewModel{ EditorViewModel(get(), get(), get()) }

    // Player
    viewModel { MiniPlayerViewModel(get()) }
    viewModel { PlayerViewModel(get(), get(), get()) }
    viewModel { QueueViewModel(get(), get()) }
}