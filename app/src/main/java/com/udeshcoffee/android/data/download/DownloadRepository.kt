package com.udeshcoffee.android.data.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.preference.PreferenceManager
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.udeshcoffee.android.data.subsonic.ServerSong
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.data.subsonic.toServerSong
import com.udeshcoffee.android.data.subsonic.toSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playback.DownloadsService
import com.udeshcoffee.android.playback.SUBSONIC_MEDIA_PREFIX
import com.udeshcoffee.android.utils.artworkData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors

enum class DownloadStatus { QUEUED, DOWNLOADING, DONE, FAILED }

/** A server song saved (or being saved) for offline listening. [progress] is 0..1. */
data class SongDownload(val song: Song, val status: DownloadStatus, val progress: Float, val updatedMs: Long)

/** How much of a set of songs (an album, a playlist) is downloaded. */
data class DownloadSummary(val total: Int, val done: Int, val active: Int, val progress: Float) {
    val allDone: Boolean get() = total > 0 && done == total
    val isDownloading: Boolean get() = active > 0
}

/** Summarises [songs] (server songs only; device songs don't download) against [downloads]. */
fun downloadSummary(songs: List<Song>, downloads: Map<String, SongDownload>): DownloadSummary {
    val ids = songs.mapNotNull { it.remoteId }.distinct()
    var done = 0
    var active = 0
    var progress = 0f
    for (id in ids) {
        val download = downloads[id] ?: continue
        when (download.status) {
            DownloadStatus.DONE -> { done++; progress += 1f }
            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> { active++; progress += download.progress }
            DownloadStatus.FAILED -> Unit
        }
    }
    return DownloadSummary(ids.size, done, active, if (ids.isEmpty()) 0f else progress / ids.size)
}

/**
 * Offline listening for music server songs. Downloads are Media3 downloads (run by
 * [DownloadsService], which shows the progress notification) into [cache], keyed by the song's
 * media ID, the same `customCacheKey` its MediaItem carries. Playback reads through the same cache
 * (`playbackMediaSourceFactory`), so a downloaded song plays from storage, without the network,
 * and everything else streams as before. Each download stores its song's details ([ServerSong]),
 * so the Downloads list works offline.
 *
 * Use from the main thread (Media3's DownloadManager lives there).
 */
@OptIn(UnstableApi::class)
class DownloadRepository(private val context: Context, private val subsonic: SubsonicRepository) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }

    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }

    /** Downloaded audio. One instance per directory and process: playback and downloads share it. */
    val cache: Cache by lazy {
        SimpleCache(File(context.getExternalFilesDir(null) ?: context.filesDir, DIRECTORY), NoOpCacheEvictor(), databaseProvider)
    }

    private val listener = object : DownloadManager.Listener {
        override fun onInitialized(downloadManager: DownloadManager) = loadAll()

        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
            val entry = download.toSongDownload()
            val remoteId = download.request.id.removePrefix(SUBSONIC_MEDIA_PREFIX)
            _downloads.update { if (entry == null) it - remoteId else it + (remoteId to entry) }
            if (download.state == Download.STATE_DOWNLOADING) watchProgress()
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            _downloads.update { it - download.request.id.removePrefix(SUBSONIC_MEDIA_PREFIX) }
        }
    }

    /** Media3's download manager (used by [DownloadsService]); created on the main thread. */
    val manager: DownloadManager by lazy {
        val newManager = DownloadManager(
            context,
            databaseProvider,
            cache,
            DefaultDataSource.Factory(context),
            Executors.newFixedThreadPool(MAX_PARALLEL)
        ).apply {
            maxParallelDownloads = MAX_PARALLEL
            requirements = requirementsFor(wifiOnly)
            addListener(listener)
        }
        newManager
    }

    private val _downloads = MutableStateFlow<Map<String, SongDownload>>(emptyMap())
    /** Downloads by server song ID (queued, in progress, done or failed). */
    val downloads: StateFlow<Map<String, SongDownload>> = _downloads.asStateFlow()

    /** IDs of the server songs that are fully downloaded. */
    val downloadedIds: StateFlow<Set<String>> = _downloads
            .map { all -> all.filterValues { it.status == DownloadStatus.DONE }.keys }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** The downloaded songs, newest download first. */
    val downloadedSongs: Flow<List<Song>> = _downloads.map { all ->
        all.values.filter { it.status == DownloadStatus.DONE }.sortedByDescending { it.updatedMs }.map { it.song }
    }

    init {
        // Creates the manager on the main thread; it loads its index and calls onInitialized.
        scope.launch {
            val initializedManager = manager

            if (initializedManager.isInitialized) loadAll()
        }
    }

    /** Only download on unmetered (Wi-Fi) networks. */
    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) {
            prefs.edit { putBoolean(KEY_WIFI_ONLY, value) }
            DownloadService.sendSetRequirements(context, DownloadsService::class.java, requirementsFor(value), false)
        }

    private fun requirementsFor(wifiOnly: Boolean) =
            Requirements(if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)

    /** Downloads the server songs among [songs] that aren't downloaded or downloading yet (failed ones retry). */
    fun download(songs: List<Song>) {
        for (song in songs) {
            val remoteId = song.remoteId ?: continue
            val existing = _downloads.value[remoteId]
            if (existing != null && existing.status != DownloadStatus.FAILED) continue
            val url = subsonic.streamUrl(remoteId) ?: continue
            val snapshot = song.toServerSong() ?: continue
            val id = SUBSONIC_MEDIA_PREFIX + remoteId
            val request = DownloadRequest.Builder(id, url.toUri())
                    .setCustomCacheKey(id)
                    .setData(json.encodeToString(ServerSong.serializer(), snapshot).toByteArray())
                    .build()
            DownloadService.sendAddDownload(context, DownloadsService::class.java, request, false)
            _downloads.update { it + (remoteId to SongDownload(song, DownloadStatus.QUEUED, 0f, System.currentTimeMillis())) }
            prefetchArtwork(song)
        }
    }

    /** Removes the downloads (finished or not) of [songs]. */
    fun remove(songs: List<Song>) {
        val ids = songs.mapNotNull { it.remoteId }.filter { it in _downloads.value }
        for (remoteId in ids) DownloadService.sendRemoveDownload(context, DownloadsService::class.java, SUBSONIC_MEDIA_PREFIX + remoteId, false)
        _downloads.update { it - ids.toSet() }
    }

    /** Removes every download (e.g. when leaving the server: the songs belong to it). */
    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, DownloadsService::class.java, false)
        _downloads.value = emptyMap()
    }

    /** Puts the cover in Coil's disk cache too, so the artwork shows offline. */
    private fun prefetchArtwork(song: Song) {
        val data = song.artworkData() ?: return
        SingletonImageLoader.get(context).enqueue(ImageRequest.Builder(context).data(data).build())
    }

    /** Reads every download from Media3's index (off the main thread). */
    private fun loadAll() {
        scope.launch {
            val all = withContext(Dispatchers.IO) {
                manager.downloadIndex.getDownloads().use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.download) }
                }
            }
            _downloads.value = all.mapNotNull { download ->
                download.toSongDownload()?.let { download.request.id.removePrefix(SUBSONIC_MEDIA_PREFIX) to it }
            }.toMap()
            watchProgress()
        }
    }

    private var progressJob: Job? = null

    /** Media3 doesn't report progress: poll the running downloads while there are any. */
    private fun watchProgress() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (true) {
                val running = manager.currentDownloads.filter { it.state == Download.STATE_DOWNLOADING }
                if (running.isEmpty()) break
                _downloads.update { all ->
                    all + running.mapNotNull { d -> d.toSongDownload()?.let { d.request.id.removePrefix(SUBSONIC_MEDIA_PREFIX) to it } }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun Download.toSongDownload(): SongDownload? {
        val status = when (state) {
            Download.STATE_COMPLETED -> DownloadStatus.DONE
            Download.STATE_FAILED -> DownloadStatus.FAILED
            Download.STATE_DOWNLOADING -> DownloadStatus.DOWNLOADING
            Download.STATE_REMOVING -> return null
            else -> DownloadStatus.QUEUED // queued, stopped, restarting, or waiting for the network
        }
        val song = runCatching { json.decodeFromString(ServerSong.serializer(), String(request.data)) }.getOrNull()?.toSong() ?: return null
        val progress = if (status == DownloadStatus.DONE) 1f else (percentDownloaded / 100f).coerceIn(0f, 1f)
        return SongDownload(song, status, progress, updateTimeMs)
    }

    companion object {
        /** Under the app's external files dir (not backed up; removed with the app). */
        const val DIRECTORY = "downloads"
        private const val KEY_WIFI_ONLY = "download_wifi_only"
        private const val MAX_PARALLEL = 3
        private const val PROGRESS_INTERVAL_MS = 500L
    }
}
