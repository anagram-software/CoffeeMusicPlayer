package com.udeshcoffee.android.ui.main.library.nested.folder

import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.queueSong
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.HistoryEntry
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.getExtSdCardFiles
import io.reactivex.disposables.Disposable
import org.koin.standalone.KoinComponent
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
* Created by Udathari on 9/5/2017.
*/
class FolderPresenter(
        private val mediaRepository: MediaRepository,
                      private val dataRepository: DataRepository
): FolderContract.Presenter, KoinComponent {

    val TAG = "FolderPresenter"

    override lateinit var view: FolderContract.View

    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    override val history: ArrayList<HistoryEntry> = ArrayList()
    override var currentDir: File? = null

    private var internal: String? = null
    private var sdCard: String? = null

    override fun start() {
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            val id = getService()?.currentSong()?.id ?: -1
                            view.setCurrentSong(id)
                        }
                    }
                }

        view.setPath("/")
        fetchRoot()
    }

    override fun stop() {
        dispose()
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun fetchFolders() {
        currentDir?.let {
            val file = it
            if (!file.canRead()) {
                if (file.absolutePath.startsWith(
                                Environment.getExternalStorageDirectory().toString())
                        || file.absolutePath.startsWith("/sdcard")
                        || file.absolutePath.startsWith("/mnt/sdcard")) {
                    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED && Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED_READ_ONLY) {
                        currentDir = file
                        val state = Environment.getExternalStorageState()
                        if (Environment.MEDIA_SHARED == state) {
                            view.showErrorToast("USB Active")
                        } else {
                            view.showErrorToast("Not Mounted")
                        }
                        view.populateItems(ArrayList(), 0)
                    }
                }
                view.showErrorToast("Access Error ${file.canRead()},${file.canWrite()},${file.path}")
            }
            var files: Array<File>? = null
            try {
                files = file.listFiles()
            } catch (e: Exception) {
                view.showErrorToast(e.localizedMessage)
            }

            if (files == null) {
                view.showErrorToast("No Files Found")
                return
            }
            currentDir = file
            Arrays.sort(files) { lhs, rhs ->
                if (lhs.isDirectory != rhs.isDirectory) {
                    return@sort if (lhs.isDirectory) -1 else 1
                }
                lhs.name.compareTo(rhs.name, ignoreCase = true)
                /*
             * long lm = lhs.lastModified(); long rm = lhs.lastModified();
             * if (lm == rm) { return 0; } else if (lm > rm) { return -1; }
             * else { return 1; }
             */
            }
            val folderItems = ArrayList<Folder>()
            for (item in files) {
                if (item.name.startsWith(".")) {
                    continue
                }
                if (item.isDirectory) {
                    val songCount = mediaRepository.getFolderSongCount(item.path + "/")
                    if (songCount > 0) {
                        folderItems.add(Folder(item.name, songCount, item))
                    }
                }
            }
            // Sorting
            val items = folderItems as List<Folder>
            SortManager.sortFolders(items)

            if (!folderSortAscending) {
                Collections.reverse(items)
            }

            view.populateItems(items, 0)
        }
    }

    private fun fetchRoot() {
        val folderItems = ArrayList<Folder>()

        val internal = Environment.getExternalStorageDirectory()
        if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
            folderItems.add(Folder("Internal", mediaRepository.getFolderSongCount(internal.path + "/"), internal))
            this.internal = internal.path
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val sdCardFiles = getExtSdCardFiles()
            for (file in sdCardFiles) {
                file.let {
                    folderItems.add(Folder("SD Card", mediaRepository.getFolderSongCount(it.path + "/"), it))
                    this.sdCard = it.path
                }
            }
        }

        //Sorting
        val items = folderItems as List<Folder>
        SortManager.sortFolders(items)

        if (!folderSortAscending) {
            Collections.reverse(items)
        }

        dispose()
        view.populateItems(items, 0)
        currentDir = null
    }

    override fun fetchSongs() {
        currentDir?.let {
            dispose()
            disposable = mediaRepository.getFolderSongs(it.path + "/")
                    .map {
                        SortManager.sortFolderSongs(it)

                        if (!sortAscending) {
                            Collections.reverse(it)
                        }

                        return@map it
                    }
                    .subscribe { view.populateItems(it) }
        }
    }

    override fun checkSortAndFetchFolders() {
        Log.d(TAG, "checkSortAndFetchFolders history: ${history.size}")
        if (currentDir == null)
            fetchRoot()
        else if (currentDir != null)
            fetchFolders()
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun upClicked() = if (history.size > 0) {
        view.showHideLoading(true)
        val prev = history.removeAt(history.size - 1)
        currentDir = prev.directory
        setPath(currentDir!!.path)
        fetchFolders()
        fetchSongs()
    } else if (history.size == 0 && currentDir != null) {
        view.showHideLoading(true)
        view.setPath("/")
        fetchRoot()
    } else {
        view.showErrorToast("Can't go back")
    }

    override fun playClick(songs: ArrayList<Song>) {
        playSong(0, songs, true)
    }

    override fun playNextClick(songs: java.util.ArrayList<Song>) {
        queueSong(songs, true)
    }

    override fun queueClick(songs: java.util.ArrayList<Song>) {
        queueSong(songs, false)
    }

    override fun addToFavoritesClick(songs: java.util.ArrayList<Song>) {
        if (songs.size > 0) {
            dataRepository.addToFavorites(songs)
            view.showFavoritesToast(true)
        } else {
            view.showFavoritesToast(false)
        }
    }

    override fun addToPlaylistClick(songs: java.util.ArrayList<Song>) {
        view.showAddToPlaylistDialog(songs)
    }


    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun folderItemClicked(item: Folder, lastVisibleItem: Int) {
        if (currentDir != null) {
            val hi = HistoryEntry(lastVisibleItem, currentDir!!)
//            hi.scrollItem = folderLayoutManger.findFirstCompletelyVisibleItemPosition()
            history.add(hi)
        }
        view.showHideLoading(true)
        currentDir = item.file
        currentDir?.let {
            setPath(it.path)
            fetchFolders()
            fetchSongs()
        }

    }

    override fun folderItemLongClicked(item: Folder) {
        view.showCollectionLongDialog(item.title, mediaRepository.getFolderSongs(item.file.path + "/"))
    }

    private fun setPath(path: String) {
        var newPath : String? = null
        internal?.let {
            if (path.contains(it))
                newPath = "/Internal" + path.substring(it.length)

        }
        sdCard?.let {
            if (path.contains(it))
                newPath = "/SD Card" + path.substring(it.length)

        }
        if (newPath == null)
            newPath = path
        view.setPath(newPath!!)
    }

    override var sortOrder: Int
        get() = SortManager.folderSongsSortOrder
        set(value) {
            SortManager.folderSongsSortOrder = value}

    override var sortAscending: Boolean
        get() = SortManager.folderSongsAscending
        set(value) {
            SortManager.folderSongsAscending = value}

    override var folderSortOrder: Int
        get() = SortManager.folderSortOrder
        set(value) {
            SortManager.folderSortOrder = value}

    override var folderSortAscending: Boolean
        get() = SortManager.foldersAscending
        set(value) { SortManager.foldersAscending = value}
}