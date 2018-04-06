package com.udeshcoffee.android.ui.main.library.nested.folder

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.HistoryEntry
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.ToastMessage
import com.udeshcoffee.android.utils.getExtSdCardFiles
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
* Created by Udathari on 9/5/2017.
*/
class FolderViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {

    val path = MutableLiveData<String>()
    val folders = MutableLiveData<List<Folder>>()

    // Events
    val showCollectionLongDialog = SingleLiveEvent<Pair<String, List<Song>>>()
    val showToast = ToastMessage()

    private var disposable: Disposable? = null

    private val history: ArrayList<HistoryEntry> = ArrayList()
    private var currentDir: File? = null

    private var internal: String? = null
    private var sdCard: String? = null

    override fun start() {
        super.start()

        path.value = "/"
        fetchRoot()
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
                            showToast.setMessage("USB Active")
                        } else {
                            showToast.setMessage("Not Mounted")
                        }
                        folders.value = ArrayList()
                    }
                }
                showToast.setMessage("Access Error ${file.canRead()},${file.canWrite()},${file.path}")
            }
            var files: Array<File>? = null
            try {
                files = file.listFiles()
            } catch (e: Exception) {
                showToast.setMessage(e.localizedMessage)
            }

            if (files == null) {
                showToast.setMessage("No Files Found")
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

            folders.value = items
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
        disposeSongs()
        folders.value = items
        currentDir = null
    }

    override fun fetchSongs() {
        currentDir?.let {
            disposeSongs()
            disposable = mediaRepository.getFolderSongs(it.path + "/")
                    .map {
                        SortManager.sortFolderSongs(it)

                        if (!songSortAscending) {
                            Collections.reverse(it)
                        }

                        return@map it
                    }
                    .subscribe {
                        songs.value = it
                    }
        }
    }

    override fun disposeSongs() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun checkSortAndFetchFolders() {
        Log.d(Companion.TAG, "checkSortAndFetchFolders history: ${history.size}")
        if (currentDir == null)
            fetchRoot()
        else if (currentDir != null)
            fetchFolders()
    }

    fun upClicked() = if (history.size > 0) {
        val prev = history.removeAt(history.size - 1)
        currentDir = prev.directory
        setPath(currentDir!!.path)
        fetchFolders()
        fetchSongs()
    } else if (history.size == 0 && currentDir != null) {
        path.value = "/"
        fetchRoot()
    } else {
        showToast.setMessage("Can't go back")
    }

    fun folderItemClicked(position: Int, lastVisibleItem: Int) {
        if (currentDir != null) {
            val hi = HistoryEntry(lastVisibleItem, currentDir!!)
            history.add(hi)
        }
        currentDir = folders.value?.get(position)?.file
        currentDir?.let {
            setPath(it.path)
            fetchFolders()
            fetchSongs()
        }
    }

    fun folderItemLongClicked(position: Int) {
        folders.value?.get(position)?.let {folder ->
            mediaRepository.getFolderSongs(folder.file.path + "/")
                    .map({ songs ->
                        SortManager.sortFolderSongs(songs)

                        if (!SortManager.folderSongsAscending) {
                            Collections.reverse(songs)
                        }

                        return@map songs
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .firstOrError()
                    .subscribe({
                        showCollectionLongDialog.value = folder.title to it
                    }, {})
        }
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
        this.path.value =  newPath
    }

    override var songSortOrder: Int
        get() = SortManager.folderSongsSortOrder
        set(value) { SortManager.folderSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.folderSongsAscending
        set(value) { SortManager.folderSongsAscending = value}

    var folderSortOrder: Int
        get() = SortManager.folderSortOrder
        set(value) {
            SortManager.folderSortOrder = value}

    var folderSortAscending: Boolean
        get() = SortManager.foldersAscending
        set(value) { SortManager.foldersAscending = value}

    companion object {
        private const val TAG = "FolderPresenter"
    }
}