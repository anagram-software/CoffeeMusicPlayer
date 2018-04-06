package com.udeshcoffee.android.di

import android.preference.PreferenceManager
import com.udeshcoffee.android.ui.main.equalizer.EqualizerContract
import com.udeshcoffee.android.ui.main.equalizer.EqualizerPresenter
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.player.lyrics.LyricsContract
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.ui.player.lyrics.LyricsPresenter
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import org.koin.dsl.module.applicationContext

/**
 * Created by Udathari on 1/14/2018.
 */
val commonModule = applicationContext {

    factory { PreferenceManager.getDefaultSharedPreferences(get()) }

    // Player
    factory { MiniPlayerFragment() }

    factory { PlayerFragment() }

    factory { QueueFragment() }

    factory { LyricsFragment() }
    factory { LyricsPresenter(get(), get()) as LyricsContract.Presenter }

    // Main
    factory { EqualizerPresenter() as EqualizerContract.Presenter }
}