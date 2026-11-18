package com.udeshcoffee.android.di

import com.udeshcoffee.android.ui.compose.detail.AlbumDetailViewModel
import com.udeshcoffee.android.ui.compose.detail.ArtistDetailViewModel
import com.udeshcoffee.android.ui.compose.detail.FavoritesViewModel
import com.udeshcoffee.android.ui.compose.detail.GenreDetailViewModel
import com.udeshcoffee.android.ui.compose.detail.PlaylistAddSongsViewModel
import com.udeshcoffee.android.ui.compose.detail.PlaylistDetailViewModel
import com.udeshcoffee.android.ui.compose.detail.PlaylistsViewModel
import com.udeshcoffee.android.ui.compose.detail.SearchViewModel
import com.udeshcoffee.android.ui.compose.editor.EditorScreenViewModel
import com.udeshcoffee.android.ui.compose.equalizer.EqualizerViewModel
import com.udeshcoffee.android.ui.compose.library.FoldersViewModel
import com.udeshcoffee.android.ui.compose.library.LibraryViewModel
import com.udeshcoffee.android.ui.compose.player.LyricsViewModel
import com.udeshcoffee.android.ui.compose.player.NowPlayingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels of the Compose UI. Parameterised ones take their IDs via parametersOf(...). */
val composeModule = module {
    viewModel { LibraryViewModel(get()) }
    viewModel { FoldersViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { PlaylistsViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { (albumId: Long) -> AlbumDetailViewModel(albumId, get()) }
    viewModel { (artistId: Long, name: String) -> ArtistDetailViewModel(artistId, name, get(), get()) }
    viewModel { (genreId: Long) -> GenreDetailViewModel(genreId, get()) }
    viewModel { (playlistId: Long, type: Int) -> PlaylistDetailViewModel(playlistId, type, get()) }
    viewModel { (playlistId: Long) -> PlaylistAddSongsViewModel(playlistId, get()) }
    viewModel { (songId: Long) -> EditorScreenViewModel(songId, androidApplication(), get(), get()) }
    viewModel { NowPlayingViewModel(get(), get(), get()) }
    viewModel { LyricsViewModel(get(), get(), get()) }
    viewModel { EqualizerViewModel() }
}
