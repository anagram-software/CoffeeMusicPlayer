package com.udeshcoffee.android.ui.player.lyrics

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import com.udeshcoffee.android.playback.PlaybackEvents
import com.udeshcoffee.android.extensions.getPlayer
import com.udeshcoffee.android.extensions.playbackEvents
import android.content.SharedPreferences
import android.util.Log
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.remote.genius.Result
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.model.Lyric
import com.udeshcoffee.android.extensions.toLastFMArtistQuery
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.PreferenceUtil
import org.koin.core.component.KoinComponent
import java.util.*

/**
 * Created by Udathari on 8/25/2017.
 */
class LyricsPresenter(private val dataRepository: DataRepository,
                      val sharedPreferences: SharedPreferences):
        LyricsContract.Presenter, KoinComponent {

    object LyricsSize {
        val SMALL = 1
        val MEDIUM = 2
        val LARGE = 3
    }

    val TAG = "LyricsPresenter"

    override lateinit var view: LyricsContract.View

    /** Lives from start() to stop(). */
    private var scope: CoroutineScope? = null
    var currentSong: Song? = null
    private var lyricJob: Job? = null

    override fun start() {
        val scope = MainScope().also { this.scope = it }
        scope.launch {
            playbackEvents()
                    .filter { it == PlaybackEvents.METADATA_CHANGED || it == PlaybackEvents.SERVICE_CONNECTED }
                    // Metadata events also fire for playback state changes; reload only for a new song.
                    .filter { getPlayer()?.currentSong()?.id != currentSong?.id }
                    .collect { fetchLyrics() }
        }
        fetchLyrics()
        view.setLyricsSize(lyricsSize)
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        lyricJob = null
    }

    override fun fetchLyrics() {
        val song = getPlayer()?.currentSong()
        currentSong = song
        song ?: return
        launchLyricJob {
            val lyrics = dataRepository.getLyrics(song)
            if (lyrics != null) view.setLyrics(lyrics)
            else searchLyrics(song.id, song.title, song.artistName, true)
        }
    }

    /** Runs [block] as the only lyrics operation (a new one cancels the previous). */
    private fun launchLyricJob(block: suspend () -> Unit) {
        lyricJob?.cancel()
        lyricJob = scope?.launch { block() }
    }

    override fun changeLyricsSize() {
        when (lyricsSize) {
            LyricsPresenter.LyricsSize.SMALL -> {
                lyricsSize = LyricsPresenter.LyricsSize.MEDIUM
                view.setLyricsSize(LyricsPresenter.LyricsSize.MEDIUM)
            }
            LyricsPresenter.LyricsSize.MEDIUM -> {
                lyricsSize = LyricsPresenter.LyricsSize.LARGE
                view.setLyricsSize(LyricsPresenter.LyricsSize.LARGE)
            }
            LyricsPresenter.LyricsSize.LARGE -> {
                lyricsSize = LyricsPresenter.LyricsSize.SMALL
                view.setLyricsSize(LyricsPresenter.LyricsSize.SMALL)
            }
        }
    }

    private suspend fun searchLyrics(id: Long, title: String, artist: String, shouldCheckEqual: Boolean) {
        Log.d(TAG, "Searching Lyrics id:$id title:$title artist:$artist")
        view.showSearching()
        val hits = dataRepository.searchLyrics(title, artist.toLastFMArtistQuery(), shouldCheckEqual)
        if (currentSong?.id != id) return
        when {
            hits.isEmpty() -> view.setNotFound()
            hits.size == 1 -> loadLyrics(id, hits[0].result.path)
            else -> view.setMultipleFound(hits.map { it.result })
        }
    }

    private suspend fun loadLyrics(id: Long, path: String) {
        view.showLoading()
        if (path.isEmpty()) return
        val lyrics = dataRepository.loadLyrics(id, path)
        if (currentSong?.id != id) return
        if (lyrics != null) view.setLyrics(lyrics) else view.setNotFound()
    }

    override fun search(id: Long, title: String, artist:String) {
        if (currentSong?.id == id) launchLyricJob { searchLyrics(id, title, artist, false) }
    }

    override fun addCustomLyrics(id: Long, lyrics: String) {
        if (currentSong?.id != id) return
        view.setLyrics(lyrics)
        scope?.launch { dataRepository.setLyrics(Lyric(id, lyrics)) }
    }

    override fun retry() {
        fetchLyrics()
    }

    override fun cancel() {

    }

    override fun selectFromMultiple(result: Result) {
        val id = currentSong?.id ?: return
        launchLyricJob { loadLyrics(id, result.path) }
    }

    override fun openSearch() {
        currentSong?.let { view.showSearchDialog(it.id, it.title, it.artistName) }
    }

    override fun openAddLyrics() {
        currentSong?.let { view.showCustomLyricDialog(it.id) }
    }

    private var lyricsSize: Int
        get() = sharedPreferences.getInt(PreferenceUtil.LYRICS_SIZE, LyricsSize.SMALL)
        set(value) {
            sharedPreferences.edit().putInt(PreferenceUtil.LYRICS_SIZE, value).apply()
        }
}