package com.udeshcoffee.android.ui.compose.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.sortedAlbums
import com.udeshcoffee.android.ui.compose.components.sortedArtists
import com.udeshcoffee.android.ui.compose.components.sortedFolders
import com.udeshcoffee.android.ui.compose.components.sortedSongs
import com.udeshcoffee.android.utils.getStorageRoots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

/** Library tabs. Lists are null until first loaded (to tell "loading" from "empty"). */
class LibraryViewModel(mediaRepository: MediaRepository) : ViewModel() {

    private fun <T> Flow<T>.state(): StateFlow<T?> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeSongs(), SortSettings.songs.state) { s, sort -> s.sortedSongs(sort) }.state()

    val albums: StateFlow<List<Album>?> =
            combine(mediaRepository.observeAlbums(), SortSettings.albums.state) { a, sort -> a.sortedAlbums(sort) }.state()

    val artists: StateFlow<List<Artist>?> =
            combine(mediaRepository.observeArtists(), SortSettings.artists.state) { a, sort -> a.sortedArtists(sort) }.state()

    val genres: StateFlow<List<Genre>?> = mediaRepository.observeGenres().state()
}

/** Folder browser: storage roots, then folders derived from MediaStore paths. */
@OptIn(ExperimentalCoroutinesApi::class)
class FoldersViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    /** Directory being shown; null = storage roots. */
    val currentDir = MutableStateFlow<File?>(null)
    private var roots: List<Pair<String, File>> = emptyList()

    val folders: StateFlow<List<Folder>?> = combine(currentDir, SortSettings.folders.state) { dir, sort -> dir to sort }
            .flatMapLatest { (dir, sort) ->
                flow {
                    val items = if (dir == null) {
                        roots = getStorageRoots(App.instance)
                        roots.map { (name, root) -> Folder(name, mediaRepository.getFolderSongCount(root.path + "/"), root) }
                    } else {
                        mediaRepository.getChildFolders(dir.path)
                                .map { (path, count) -> File(path).let { Folder(it.name, count, it) } }
                                .filterNot { it.title.startsWith(".") }
                    }
                    emit(items.sortedFolders(sort))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>> = combine(currentDir, SortSettings.folderSongs.state) { dir, sort -> dir to sort }
            .flatMapLatest { (dir, sort) ->
                if (dir == null) flowOf(emptyList())
                else mediaRepository.observeFolderSongs(dir.path + "/").map { it.sortedSongs(sort) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** "/Internal/Music"-style label for the current directory. */
    fun pathLabel(dir: File?): String {
        dir ?: return "Storage"
        val root = roots.firstOrNull { (_, r) -> dir.path.startsWith(r.path) }
        return root?.let { (name, r) -> "/$name" + dir.path.substring(r.path.length) } ?: dir.path
    }

    fun open(folder: Folder) { currentDir.value = folder.file }

    /** Goes up one level; false when already at the roots. */
    fun up(): Boolean {
        val dir = currentDir.value ?: return false
        currentDir.value = if (roots.any { it.second == dir }) null else dir.parentFile
        return true
    }

    suspend fun folderSongs(folder: Folder): List<Song> =
            mediaRepository.loadFolderSongs(folder.file.path + "/").sortedSongs(SortSettings.folderSongs.state.value)
}
