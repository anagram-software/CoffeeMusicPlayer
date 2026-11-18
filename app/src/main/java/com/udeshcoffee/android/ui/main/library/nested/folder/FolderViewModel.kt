package com.udeshcoffee.android.ui.main.library.nested.folder

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.app.Application
import androidx.lifecycle.MutableLiveData
import android.os.Build
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
import com.udeshcoffee.android.App
import com.udeshcoffee.android.utils.getStorageRoots
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
) : DetailSongViewModel(application, dataRepository) {

    val path = MutableLiveData<String>()
    val folders = MutableLiveData<List<Folder>>()

    // Events
    val showCollectionLongDialog = SingleLiveEvent<Pair<String, List<Song>>>()
    val showToast = ToastMessage()


    private val history: ArrayList<HistoryEntry> = ArrayList()
    private var currentDir: File? = null

    /** Mounted storage volumes as (display name, root directory). */
    private var roots: List<Pair<String, File>> = emptyList()

    override fun start() {
        super.start()

        path.value = "/"
        fetchRoot()
    }

    private fun fetchFolders() {
        val dir = currentDir ?: return
        viewModelScope.launch {
            val folderItems = mediaRepository.getChildFolders(dir.path)
                    .map { (path, count) -> File(path).let { Folder(it.name, count, it) } }
                    .filterNot { it.title.startsWith(".") }
            if (currentDir != dir) return@launch
            folders.value = folderItems.sortedWith(SortManager::sortFolders, folderSortAscending)
        }
    }

    private fun fetchRoot() {
        disposeSongs()
        currentDir = null
        viewModelScope.launch {
            roots = getStorageRoots(App.instance)
            val folderItems = roots.map { (name, dir) ->
                Folder(name, mediaRepository.getFolderSongCount(dir.path + "/"), dir)
            }
            if (currentDir != null) return@launch
            folders.value = folderItems.sortedWith(SortManager::sortFolders, folderSortAscending)
        }
    }

    override fun fetchSongs() {
        val dir = currentDir ?: return
        collectSongs(mediaRepository.observeFolderSongs(dir.path + "/"), SortManager::sortFolderSongs) { songSortAscending }
    }

    fun checkSortAndFetchFolders() {
        Log.d(TAG, "checkSortAndFetchFolders history: ${history.size}")
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
        val folder = folders.value?.getOrNull(position) ?: return
        viewModelScope.launch {
            val songs = mediaRepository.loadFolderSongs(folder.file.path + "/")
                    .sortedWith(SortManager::sortFolderSongs, SortManager.folderSongsAscending)
            showCollectionLongDialog.value = folder.title to songs
        }
    }

    private fun setPath(path: String) {
        val root = roots.firstOrNull { (_, dir) -> path.startsWith(dir.path) }
        this.path.value = root?.let { (name, dir) -> "/$name" + path.substring(dir.path.length) } ?: path
    }

    override var songSortOrder: Int
        get() = SortManager.folderSongsSortOrder
        set(value) {
            SortManager.folderSongsSortOrder = value
        }

    override var songSortAscending: Boolean
        get() = SortManager.folderSongsAscending
        set(value) {
            SortManager.folderSongsAscending = value
        }

    var folderSortOrder: Int
        get() = SortManager.folderSortOrder
        set(value) {
            SortManager.folderSortOrder = value
        }

    var folderSortAscending: Boolean
        get() = SortManager.foldersAscending
        set(value) {
            SortManager.foldersAscending = value
        }

    companion object {
        private const val TAG = "FolderPresenter"
    }
}