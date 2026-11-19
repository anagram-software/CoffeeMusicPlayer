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
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
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
import com.udeshcoffee.android.ui.compose.navigation.SettingsRoute
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
    /** [onServer]: create it on the music server (the songs must be server songs). */
    data class NewPlaylist(val songs: List<Song>, val onServer: Boolean) : AppDialog
    data class RenamePlaylist(val playlist: Playlist) : AppDialog
    data class DeletePlaylist(val playlist: Playlist, val goBack: Boolean) : AppDialog
}

/**
 * App-wide actions available to every screen through [LocalAppActions]: playback, navigation,
 * and the shared song/collection options and playlist dialogs.
 */
class AppActions(
        private val context: Context,
        /** The music source is the server (see MusicSourceStore); new playlists go there. */
        val isServer: Boolean,
        private val navController: NavController,
        private val scope: CoroutineScope,
        private val dataRepository: DataRepository,
        private val mediaRepository: MediaRepository,
        private val subsonic: SubsonicRepository,
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

    fun playAlbum(album: Album) = launch { play(albumSongs(album)) }
    fun playArtist(artist: Artist) = launch { play(artistSongs(artist)) }
    fun albumOptions(album: Album) = launch { sheet = OptionsSheet.ForCollection(album.title, albumSongs(album)) }
    fun artistOptions(artist: Artist) = launch { sheet = OptionsSheet.ForCollection(artist.name, artistSongs(artist)) }
    fun genreOptions(genre: Genre) = launch {
        sheet = OptionsSheet.ForCollection(genre.name, if (genre.remote) subsonic.genreSongs(genre.name) else mediaRepository.loadGenreSongs(genre.id))
    }
    fun playPlaylist(playlist: Playlist) = launch { play(playlistSongs(playlist)) }
    fun playlistOptions(playlist: Playlist) = launch {
        sheet = OptionsSheet.ForCollection(playlist.title, playlistSongs(playlist), playlist.takeIf { it.type == Playlist.USER })
    }

    private suspend fun playlistSongs(playlist: Playlist): List<Song> =
            playlist.remoteId?.let { subsonic.playlistSongs(it) } ?: mediaRepository.loadPlaylistSongs(playlist.id, playlist.type)

    private suspend fun albumSongs(album: Album): List<Song> =
            album.remoteId?.let { subsonic.albumSongs(it) } ?: mediaRepository.loadAlbumSongs(album.id).sortedBy { it.track }

    private suspend fun artistSongs(artist: Artist): List<Song> =
            artist.remoteId?.let { subsonic.artistSongs(it) } ?: mediaRepository.loadArtistSongs(artist.id)

    /** Runs [block], reporting music server failures as a message instead of crashing. */
    private fun launch(block: suspend () -> Unit) = scope.launch {
        try {
            block()
        } catch (e: SubsonicException) {
            message(e.message ?: "Couldn't reach the music server")
        }
    }

    // Sheets and dialogs
    fun songOptions(song: Song, playlist: Playlist? = null) { sheet = OptionsSheet.ForSong(song, playlist) }
    fun collectionOptions(title: String, songs: List<Song>) { sheet = OptionsSheet.ForCollection(title, songs) }
    fun addToPlaylist(songs: List<Song>, excludePlaylistId: Long? = null) { if (songs.isNotEmpty()) dialog = AppDialog.AddToPlaylist(songs, excludePlaylistId) }
    fun newPlaylist(songs: List<Song> = emptyList()) { dialog = AppDialog.NewPlaylist(songs, onServer = isServer) }
    fun renamePlaylist(playlist: Playlist) { dialog = AppDialog.RenamePlaylist(playlist) }
    fun deletePlaylist(playlist: Playlist, goBack: Boolean = false) { dialog = AppDialog.DeletePlaylist(playlist, goBack) }

    // Navigation
    private fun open(route: Any) {
        setPlayerExpanded(false)
        navController.navigate(route) { launchSingleTop = true }
    }

    fun openAlbum(album: Album) = open(AlbumRoute(album.id, album.title, album.remoteId))
    fun openAlbum(song: Song) {
        if (song.isRemote && song.albumRemoteId == null) return
        open(AlbumRoute(song.albumId, song.albumName, song.albumRemoteId))
    }
    fun openArtist(artist: Artist) = open(ArtistRoute(artist.id, artist.name, artist.remoteId))
    fun openArtist(song: Song) {
        if (song.isRemote && song.artistRemoteId == null) return
        open(ArtistRoute(song.artistId, song.artistName, song.artistRemoteId))
    }
    fun openGenre(genre: Genre) = open(GenreRoute(genre.id, genre.name, genre.remote))
    fun openPlaylist(playlist: Playlist) = open(PlaylistRoute(playlist.id, playlist.type, playlist.title, playlist.remoteId, playlist.readonly))
    fun openPlaylistAddSongs(playlist: Playlist) = open(PlaylistAddSongsRoute(playlist.id, playlist.title))
    fun openSettings() = open(SettingsRoute)
    fun openEditor(song: Song) = open(EditorRoute(song.id))
    fun back() = navController.popBackStack()

    // Song actions
    fun deleteSongs(songs: List<Song>) = onDeleteSongs(songs)

    /** Device songs are favorited in the app's database; server songs are starred on the server. */
    fun toggleFavorite(song: Song) = launch {
        val remote = song.remoteId
        val added = if (remote != null) {
            val star = remote !in subsonic.starredIds.value
            subsonic.setStarred(listOf(remote), star)
            star
        } else dataRepository.toggleFavorite(song.id)
        message(if (added) "Added to favorites" else "Removed from favorites")
    }

    fun addToFavorites(songs: List<Song>) = launch {
        val (remote, local) = songs.partition { it.isRemote }
        if (local.isNotEmpty()) dataRepository.addToFavorites(local)
        subsonic.setStarred(remote.mapNotNull { it.remoteId }, true)
        message("Added to favorites")
    }

    fun share(song: Song) {
        val intent = Intent(Intent.ACTION_SEND)
                .setType("audio/*")
                .putExtra(Intent.EXTRA_STREAM, MediaAccess.songUri(song.id))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // Playlist edits. Device playlists hold device songs, server playlists hold server songs
    // (with one music source at a time, songs from the other kind only show up by accident).
    fun createPlaylist(name: String, songs: List<Song>, onServer: Boolean = false) = launch {
        val usable = songs.filter { it.isRemote == onServer }
        if (onServer) subsonic.createPlaylist(name, usable.mapNotNull { it.remoteId })
        else {
            val id = mediaRepository.addPlaylist(name)
            if (usable.isNotEmpty()) mediaRepository.addToPlaylist(id, usable)
        }
        message(skippedNote(if (songs.isEmpty()) "Created $name" else "Added to $name", songs.size - usable.size))
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: List<Song>) = launch {
        val remote = playlist.remoteId
        val usable = songs.filter { it.isRemote == (remote != null) }
        if (remote != null) subsonic.addToPlaylist(remote, usable.mapNotNull { it.remoteId })
        else mediaRepository.addToPlaylist(playlist.id, usable)
        message(skippedNote("Added to ${playlist.title}", songs.size - usable.size))
    }

    private fun skippedNote(text: String, skipped: Int) = when (skipped) {
        0 -> text
        1 -> "$text (1 song skipped)"
        else -> "$text ($skipped songs skipped)"
    }

    /** Removes one entry from a user playlist ([Song.playlistSongId]: the entry ID, or the position for server playlists). */
    fun removeFromPlaylist(playlist: Playlist, song: Song) = launch {
        val remote = playlist.remoteId
        if (remote != null) subsonic.removeFromPlaylist(remote, song.playlistSongId.toInt())
        else mediaRepository.removePlaylistItem(playlist.id, song.playlistSongId)
    }

    fun applyRename(playlist: Playlist, name: String) = launch {
        playlist.remoteId?.let { subsonic.renamePlaylist(it, name) } ?: mediaRepository.renamePlaylist(playlist.id, name)
    }

    fun applyDelete(playlist: Playlist, goBack: Boolean) = launch {
        playlist.remoteId?.let { subsonic.deletePlaylist(it) } ?: mediaRepository.deletePlaylist(playlist.id)
        message("Deleted ${playlist.title}")
        if (goBack) back()
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
}

val LocalAppActions = staticCompositionLocalOf<AppActions> { error("AppActions not provided") }
