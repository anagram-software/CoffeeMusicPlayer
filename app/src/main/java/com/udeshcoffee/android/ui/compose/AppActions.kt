package com.udeshcoffee.android.ui.compose

import android.content.Context
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.connectedPlayer
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.navigation.AlbumRoute
import com.udeshcoffee.android.ui.compose.navigation.ArtistRoute
import com.udeshcoffee.android.ui.compose.navigation.EditorRoute
import com.udeshcoffee.android.ui.compose.navigation.GenreRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistAddSongsRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistRoute
import com.udeshcoffee.android.ui.compose.navigation.SearchRoute
import com.udeshcoffee.android.utils.MediaAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A bottom sheet of actions, shown by the app scaffold. */
sealed interface OptionsSheet {
    data class ForSong(val song: Song, val playlist: Playlist? = null) : OptionsSheet
    data class ForCollection(val title: String, val songs: List<Song>, val playlist: Playlist? = null) : OptionsSheet
}

/** A dialog shown by the app scaffold. */
sealed interface AppDialog {
    data class AddToPlaylist(val songs: List<Song>, val excludePlaylistId: Long? = null) : AppDialog
    data class NewPlaylist(val songs: List<Song> = emptyList()) : AppDialog
    data class RenamePlaylist(val playlist: Playlist) : AppDialog
    data class DeletePlaylist(val playlist: Playlist, val goBack: Boolean) : AppDialog
}

/**
 * App-wide actions available to every screen through [LocalAppActions]: playback, navigation,
 * and the shared song/collection options and playlist dialogs.
 */
class AppActions(
        private val context: Context,
        private val navController: NavController,
        private val scope: CoroutineScope,
        private val dataRepository: DataRepository,
        private val mediaRepository: MediaRepository,
        val snackbar: SnackbarHostState,
        /** Deletes songs from the device (asks for consent; implemented by the activity). */
        private val onDeleteSongs: (List<Song>) -> Unit,
        /** Expands or collapses the full player. */
        val setPlayerExpanded: (Boolean) -> Unit
) {
    var sheet by mutableStateOf<OptionsSheet?>(null)
    var dialog by mutableStateOf<AppDialog?>(null)

    // Playback
    fun play(songs: List<Song>, index: Int = 0) {
        if (songs.isNotEmpty()) connectedPlayer()?.setQueue(songs, index, true)
    }

    fun shuffle(songs: List<Song>) = play(songs.shuffled())

    fun queue(songs: List<Song>, next: Boolean) {
        if (songs.isEmpty()) return
        connectedPlayer()?.addToQueue(songs, next)
        message(if (next) "Playing next" else "Added to queue")
    }

    fun playAlbum(album: Album) = scope.launch { play(mediaRepository.loadAlbumSongs(album.id).sortedBy { it.track }) }
    fun playArtist(artist: Artist) = scope.launch { play(mediaRepository.loadArtistSongs(artist.id)) }
    fun albumOptions(album: Album) = scope.launch { sheet = OptionsSheet.ForCollection(album.title, mediaRepository.loadAlbumSongs(album.id)) }
    fun artistOptions(artist: Artist) = scope.launch { sheet = OptionsSheet.ForCollection(artist.name, mediaRepository.loadArtistSongs(artist.id)) }
    fun genreOptions(genre: Genre) = scope.launch { sheet = OptionsSheet.ForCollection(genre.name, mediaRepository.loadGenreSongs(genre.id)) }
    fun playlistOptions(playlist: Playlist) = scope.launch {
        sheet = OptionsSheet.ForCollection(playlist.title, mediaRepository.loadPlaylistSongs(playlist.id, playlist.type),
                playlist.takeIf { it.type == Playlist.USER })
    }

    // Sheets and dialogs
    fun songOptions(song: Song, playlist: Playlist? = null) { sheet = OptionsSheet.ForSong(song, playlist) }
    fun collectionOptions(title: String, songs: List<Song>) { sheet = OptionsSheet.ForCollection(title, songs) }
    fun addToPlaylist(songs: List<Song>, excludePlaylistId: Long? = null) { if (songs.isNotEmpty()) dialog = AppDialog.AddToPlaylist(songs, excludePlaylistId) }
    fun newPlaylist(songs: List<Song> = emptyList()) { dialog = AppDialog.NewPlaylist(songs) }
    fun renamePlaylist(playlist: Playlist) { dialog = AppDialog.RenamePlaylist(playlist) }
    fun deletePlaylist(playlist: Playlist, goBack: Boolean = false) { dialog = AppDialog.DeletePlaylist(playlist, goBack) }

    // Navigation
    private fun open(route: Any) {
        setPlayerExpanded(false)
        navController.navigate(route) { launchSingleTop = true }
    }

    fun openAlbum(album: Album) = open(AlbumRoute(album.id, album.title))
    fun openAlbum(song: Song) = open(AlbumRoute(song.albumId, song.albumName))
    fun openArtist(artist: Artist) = open(ArtistRoute(artist.id, artist.name))
    fun openArtist(song: Song) = open(ArtistRoute(song.artistId, song.artistName))
    fun openGenre(genre: Genre) = open(GenreRoute(genre.id, genre.name))
    fun openPlaylist(playlist: Playlist) = open(PlaylistRoute(playlist.id, playlist.type, playlist.title))
    fun openPlaylistAddSongs(playlist: Playlist) = open(PlaylistAddSongsRoute(playlist.id, playlist.title))
    fun openSearch() = open(SearchRoute)
    fun openEditor(song: Song) = open(EditorRoute(song.id))
    fun back() = navController.popBackStack()

    // Song actions
    fun deleteSongs(songs: List<Song>) = onDeleteSongs(songs)

    fun toggleFavorite(song: Song) = scope.launch {
        message(if (dataRepository.toggleFavorite(song.id)) "Added to favorites" else "Removed from favorites")
    }

    fun addToFavorites(songs: List<Song>) = scope.launch {
        dataRepository.addToFavorites(songs)
        message("Added to favorites")
    }

    fun share(song: Song) {
        val intent = Intent(Intent.ACTION_SEND)
                .setType("audio/*")
                .putExtra(Intent.EXTRA_STREAM, MediaAccess.songUri(song.id))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // Playlist edits
    fun createPlaylist(name: String, songs: List<Song>) = scope.launch {
        val id = mediaRepository.addPlaylist(name)
        if (songs.isNotEmpty()) mediaRepository.addToPlaylist(id, songs)
        message(if (songs.isEmpty()) "Created $name" else "Added to $name")
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: List<Song>) = scope.launch {
        mediaRepository.addToPlaylist(playlist.id, songs)
        message("Added to ${playlist.title}")
    }

    /** Removes one entry ([Song.playlistSongId]) from a user playlist. */
    fun removeFromPlaylist(playlist: Playlist, song: Song) = scope.launch {
        mediaRepository.removePlaylistItem(playlist.id, song.playlistSongId)
    }

    fun applyRename(playlist: Playlist, name: String) = scope.launch { mediaRepository.renamePlaylist(playlist.id, name) }

    fun applyDelete(playlist: Playlist, goBack: Boolean) = scope.launch {
        mediaRepository.deletePlaylist(playlist.id)
        message("Deleted ${playlist.title}")
        if (goBack) back()
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
}

val LocalAppActions = staticCompositionLocalOf<AppActions> { error("AppActions not provided") }
