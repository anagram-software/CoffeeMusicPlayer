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
import com.udeshcoffee.android.ui.compose.home.HomeViewModel
import com.udeshcoffee.android.ui.compose.home.ListenBrainzStatsViewModel
import com.udeshcoffee.android.ui.compose.library.LibraryViewModel
import com.udeshcoffee.android.ui.compose.library.LibraryOverviewViewModel
import com.udeshcoffee.android.ui.compose.library.ServerLibraryViewModel
import com.udeshcoffee.android.ui.compose.player.LyricsViewModel
import com.udeshcoffee.android.ui.compose.player.NowPlayingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels of the Compose UI. Parameterised ones take their IDs via parametersOf(...). */
val composeModule = module {
    viewModel { LibraryViewModel(get()) }
    viewModel { LibraryOverviewViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ListenBrainzStatsViewModel(get(), get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get(), get()) }
    viewModel { PlaylistsViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { (albumId: Long, remoteId: String?) -> AlbumDetailViewModel(albumId, remoteId, get(), get()) }
    viewModel { (artistId: Long, name: String, remoteId: String?) -> ArtistDetailViewModel(artistId, name, remoteId, get(), get()) }
    viewModel { (genreId: Long, name: String, remote: Boolean) -> GenreDetailViewModel(genreId, name, remote, get(), get()) }
    viewModel { (playlistId: Long, type: Int, remoteId: String?) -> PlaylistDetailViewModel(playlistId, type, remoteId, get(), get(), get()) }
    viewModel { (playlistId: Long) -> PlaylistAddSongsViewModel(playlistId, get()) }
    viewModel { (songId: Long) -> EditorScreenViewModel(songId, androidApplication(), get(), get()) }
    viewModel { NowPlayingViewModel(get(), get(), get(), get()) }
    viewModel { LyricsViewModel(get(), get(), get()) }
    viewModel { ServerLibraryViewModel(get()) }
}
