package com.udeshcoffee.android.di

import android.preference.PreferenceManager
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailContract
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailContract
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailContract
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailPresenter
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailPresenter
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailPresenter
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailContract
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailPresenter
import com.udeshcoffee.android.ui.main.editor.EditorContract
import com.udeshcoffee.android.ui.main.editor.EditorPresenter
import com.udeshcoffee.android.ui.main.equalizer.EqualizerContract
import com.udeshcoffee.android.ui.main.equalizer.EqualizerPresenter
import com.udeshcoffee.android.ui.main.favorites.FavoritesContract
import com.udeshcoffee.android.ui.main.favorites.FavoritesPresenter
import com.udeshcoffee.android.ui.main.library.nested.folder.FolderContract
import com.udeshcoffee.android.ui.main.library.nested.folder.FolderPresenter
import com.udeshcoffee.android.ui.main.library.nested.track.TrackContract
import com.udeshcoffee.android.ui.main.library.nested.track.TrackPresenter
import com.udeshcoffee.android.ui.main.search.SearchContract
import com.udeshcoffee.android.ui.main.search.SearchPresenter
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerContract
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerPresenter
import com.udeshcoffee.android.ui.player.lyrics.LyricsContract
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.ui.player.lyrics.LyricsPresenter
import com.udeshcoffee.android.ui.player.player.PlayerContract
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.player.PlayerPresenter
import com.udeshcoffee.android.ui.player.queue.QueueContract
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import com.udeshcoffee.android.ui.player.queue.QueuePresenter
import org.koin.dsl.module.applicationContext

/**
 * Created by Udathari on 1/14/2018.
 */
val commonModule = applicationContext {

    factory { PreferenceManager.getDefaultSharedPreferences(get()) }

    // Player
    factory { MiniPlayerFragment() }
    factory { MiniPlayerPresenter() as MiniPlayerContract.Presenter }

    factory { PlayerFragment() }
    factory { PlayerPresenter(get(), get()) as PlayerContract.Presenter }

    factory { QueueFragment() }
    factory { QueuePresenter() as QueueContract.Presenter }

    factory { LyricsFragment() }
    factory { LyricsPresenter(get(), get()) as LyricsContract.Presenter }

    // Detail
    factory { AlbumDetailPresenter(get(), get()) as AlbumDetailContract.Presenter }

    factory { ArtistDetailPresenter(get(), get()) as ArtistDetailContract.Presenter }

    factory { GenreDetailPresenter(get(), get()) as GenreDetailContract.Presenter }

    factory { PlaylistDetailPresenter(get(), get()) as PlaylistDetailContract.Presenter }

    // Main
    factory { FavoritesPresenter(get()) as FavoritesContract.Presenter }

    factory { EqualizerPresenter() as EqualizerContract.Presenter }

    factory { TrackPresenter(get()) as TrackContract.Presenter }

    factory { FolderPresenter(get(), get()) as FolderContract.Presenter }

    // Features
    factory { SearchPresenter(get()) as SearchContract.Presenter }

    factory { EditorPresenter(get(), get()) as EditorContract.Presenter }

}