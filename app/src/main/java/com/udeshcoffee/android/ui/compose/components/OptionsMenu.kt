package com.udeshcoffee.android.ui.compose.components

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.download.DownloadStatus
import com.udeshcoffee.android.data.download.downloadSummary
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.AppActions
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.OptionsMenu
import com.udeshcoffee.android.ui.compose.OptionsTarget
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** A long press opens its menu at the finger if it went down this recently; otherwise under the element. */
private const val TOUCH_ANCHOR_MS = 2_000L

/**
 * Where an options menu opens for one element. Apply [modifier] to the button or item, and wrap the
 * action that opens the menu with [open] (the menu drops down from the element, for buttons) or
 * [openAtTouch] (at the finger, for long presses; under the element when there was no touch, as
 * with TalkBack).
 */
@Stable
class OptionsAnchor internal constructor(private val actions: AppActions) {
    private var bounds = Rect.Zero
    private var origin = Offset.Zero
    private var press: Offset? = null
    private var pressedAt = 0L

    val modifier: Modifier = Modifier
            .onGloballyPositioned {
                bounds = it.boundsInWindow()
                origin = it.positionInWindow()
            }
            // Observes (never consumes) presses, before the element's own click handling.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    press = down.position
                    pressedAt = SystemClock.uptimeMillis()
                }
            }

    fun open(block: () -> Unit): () -> Unit = {
        actions.menuAnchor = bounds
        block()
    }

    fun openAtTouch(block: () -> Unit): () -> Unit = {
        val p = press?.takeIf { SystemClock.uptimeMillis() - pressedAt < TOUCH_ANCHOR_MS }
        actions.menuAnchor = if (p != null) Rect(origin + p, Size.Zero) else bounds
        press = null
        block()
    }
}

@Composable
fun rememberOptionsAnchor(): OptionsAnchor {
    val actions = LocalAppActions.current
    return remember(actions) { OptionsAnchor(actions) }
}

/** One menu entry; [destructive] ones are shown in the error colour. */
private class MenuAction(val icon: ImageVector, val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * Song or collection options as a Material 3 Expressive grouped menu, opened from the app bar's
 * overflow buttons, song rows' option buttons and long presses ([OptionsAnchor] says where).
 * Shown by the app scaffold over everything; fill the screen with it.
 */
@Composable
fun OptionsMenuHost(actions: AppActions) {
    // Keep the last menu composed after it closes, so it can animate out.
    var shown by remember { mutableStateOf<OptionsMenu?>(null) }
    actions.menu?.let { shown = it }
    val menu = shown ?: return
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize().onGloballyPositioned { hostOrigin = it.positionInWindow() }) {
        // A box over the anchor, so the menu positions itself against it (below, or above when there's no room).
        val anchor = menu.anchor
        Box(Modifier
                .offset { IntOffset((anchor.left - hostOrigin.x).roundToInt(), (anchor.top - hostOrigin.y).roundToInt()) }
                .size(with(density) { anchor.width.toDp() }, with(density) { anchor.height.toDp() })) {
            val dismiss = { actions.menu = null }
            DropdownMenuPopup(expanded = actions.menu != null, onDismissRequest = dismiss) {
                val groups = when (val target = menu.target) {
                    is OptionsTarget.ForSong -> songActions(target, actions)
                    is OptionsTarget.ForCollection -> collectionActions(target, actions)
                }.filter { it.isNotEmpty() }
                // Long menus scroll on short screens.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    groups.forEachIndexed { g, group ->
                        if (g > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                        DropdownMenuGroup(shapes = MenuDefaults.groupShape(g, groups.size)) {
                            group.forEachIndexed { i, action ->
                                val colors = if (action.destructive) MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error) else MenuDefaults.itemColors()
                                DropdownMenuItem(
                                        onClick = { dismiss(); action.onClick() },
                                        text = { Text(action.label) },
                                        shape = MenuDefaults.itemShape(i, group.size).shape,
                                        colors = colors,
                                        leadingIcon = { Icon(action.icon, contentDescription = null, modifier = Modifier.size(MenuDefaults.LeadingIconSize)) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun isFavorite(song: Song): Boolean {
    val remote = song.remoteId
    return if (remote != null) {
        val subsonic: SubsonicRepository = koinInject()
        LaunchedEffect(Unit) { subsonic.ensureStarredLoaded() }
        val starred by subsonic.starredIds.collectAsStateWithLifecycle()
        remote in starred
    } else {
        val data: DataRepository = koinInject()
        val favorite by remember(song.id) { data.observeIsFavorite(song.id) }.collectAsStateWithLifecycle(false)
        favorite
    }
}

/** Play, collect, go to, and file actions for one song. */
@Composable
private fun songActions(target: OptionsTarget.ForSong, actions: AppActions): List<List<MenuAction>> {
    val song = target.song
    val downloads by koinInject<DownloadRepository>().downloads.collectAsStateWithLifecycle()
    val favorite = isFavorite(song)
    val songs = listOf(song)
    return listOf(
            listOf(
                    MenuAction(Icons.Default.PlayArrow, "Play") { actions.play(songs) },
                    MenuAction(Icons.Default.SkipNext, "Play next") { actions.queue(songs, next = true) },
                    MenuAction(QueueIcon, "Add to queue") { actions.queue(songs, next = false) }),
            listOfNotNull(
                    MenuAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { actions.addToPlaylist(songs) },
                    target.playlist?.takeIf { !it.readonly }?.let { playlist ->
                        MenuAction(Icons.Default.RemoveCircleOutline, "Remove from ${playlist.title}") { actions.removeFromPlaylist(playlist, song) }
                    },
                    if (favorite) MenuAction(Icons.Default.Favorite, "Remove from favorites") { actions.toggleFavorite(song) }
                    else MenuAction(Icons.Default.FavoriteBorder, "Add to favorites") { actions.toggleFavorite(song) },
                    // Offline listening, for server songs.
                    if (!song.isRemote) null else when (downloads[song.remoteId]?.status) {
                        DownloadStatus.DONE -> MenuAction(Icons.Default.DownloadDone, "Remove download") { actions.removeDownloads(songs) }
                        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING ->
                            MenuAction(Icons.Default.Close, "Stop download") { actions.removeDownloads(songs) }
                        else -> MenuAction(Icons.Default.Download, "Download") { actions.download(songs) }
                    }),
            listOfNotNull(
                    MenuAction(Icons.Default.Album, "Go to album") { actions.openAlbum(song) }
                            .takeIf { !song.isRemote || song.albumRemoteId != null },
                    MenuAction(Icons.Default.Person, "Go to artist") { actions.openArtist(song) }
                            .takeIf { !song.isRemote || song.artistRemoteId != null }),
            // File operations only make sense for songs on this device.
            if (song.isRemote) emptyList() else listOf(
                    MenuAction(Icons.Default.Edit, "Edit tags") { actions.openEditor(song) },
                    MenuAction(Icons.Default.Share, "Share") { actions.share(song) },
                    MenuAction(Icons.Default.Delete, "Delete from device", destructive = true) { actions.deleteSongs(songs) }))
}

/** Play, collect and playlist actions for an album, artist, genre or playlist. */
@Composable
private fun collectionActions(target: OptionsTarget.ForCollection, actions: AppActions): List<List<MenuAction>> {
    val songs = target.songs
    val downloads by koinInject<DownloadRepository>().downloads.collectAsStateWithLifecycle()
    val summary = downloadSummary(songs, downloads)
    return listOf(
            listOf(
                    MenuAction(Icons.Default.PlayArrow, "Play") { actions.play(songs) },
                    MenuAction(Icons.Default.SkipNext, "Play next") { actions.queue(songs, next = true) },
                    MenuAction(QueueIcon, "Add to queue") { actions.queue(songs, next = false) }),
            listOfNotNull(
                    MenuAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { actions.addToPlaylist(songs, target.playlist?.id) },
                    MenuAction(Icons.Default.FavoriteBorder, "Add to favorites") { actions.addToFavorites(songs) },
                    MenuAction(Icons.Default.Download, "Download") { actions.download(songs) }
                            .takeIf { summary.total > 0 && summary.done + summary.active < summary.total },
                    MenuAction(Icons.Default.DownloadDone, "Remove downloads") { actions.removeDownloads(songs) }
                            .takeIf { summary.done + summary.active > 0 }),
            target.playlist?.takeIf { !it.readonly }?.let { playlist ->
                listOf(
                        MenuAction(Icons.Default.Edit, "Rename playlist") { actions.renamePlaylist(playlist) },
                        MenuAction(Icons.Default.Delete, "Delete playlist", destructive = true) {
                            actions.deletePlaylist(playlist, goBack = target.fromPlaylistPage)
                        })
            }.orEmpty(),
            target.extras.map { MenuAction(it.icon, it.label, onClick = it.onClick) })
}
