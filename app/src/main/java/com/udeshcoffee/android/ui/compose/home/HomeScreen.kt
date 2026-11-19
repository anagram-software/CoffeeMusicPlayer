package com.udeshcoffee.android.ui.compose.home

import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.compose.AppActions
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumArtwork
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.MediaCard
import com.udeshcoffee.android.ui.compose.components.SectionHeader
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.detail.ServerNotice
import com.udeshcoffee.android.ui.compose.components.PlaylistArtwork
import com.udeshcoffee.android.ui.compose.components.icon
import com.udeshcoffee.android.utils.CoverArt
import com.udeshcoffee.android.utils.artworkData
import org.koin.compose.viewmodel.koinViewModel

private val CardWidth = 150.dp

/** Home: recently added, recently played, most played and playlists of the current music source. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    // On the device, the album rows come from the smart playlists, which "See all" opens.
    fun seeAll(type: Int): (() -> Unit)? = if (viewModel.isServer) null else ({ actions.openPlaylist(Playlist(type)) })

    TopLevelScaffold("Home") { padding ->
        val c = content
        when {
            c == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
            // Offline (or the server is down): downloaded songs still play.
            c.error != null -> androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                ServerNotice("Couldn't load: ${c.error}", viewModel::retry)
                val downloads = Playlist(Playlist.DOWNLOADS)
                androidx.compose.material3.ListItem(
                        onClick = { actions.openPlaylist(downloads) },
                        leadingContent = { Icon(downloads.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        supportingContent = { Text("Play what's saved on this device") }) { Text("Downloaded songs") }
            }
            c.isEmpty -> EmptyState("Your recently added and played music shows up here", Modifier.padding(padding), Icons.Default.Home)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 16.dp)) {
                albumSection("Recently added", c.recentlyAdded, actions, seeAll(Playlist.RECENTLY_ADDED))
                albumSection("Recently played", c.recentlyPlayed, actions, seeAll(Playlist.RECENTLY_PLAYED))
                albumSection("Most played", c.mostPlayed, actions, seeAll(Playlist.MOST_PLAYED))
                playlistSection(c.playlists, actions)
            }
        }
    }
}

private fun LazyListScope.sectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    item(key = "header:$title") {
        SectionHeader(title) { onSeeAll?.let { TextButton(onClick = it) { Text("See all") } } }
    }
}

private fun LazyListScope.albumSection(title: String, albums: List<Album>, actions: AppActions, onSeeAll: (() -> Unit)?) {
    if (albums.isEmpty()) return
    sectionHeader(title, onSeeAll)
    item(key = "row:$title") {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(albums, key = { it.id }) { album ->
                // Just the artist: song counts here would only count the recent / played songs.
                MediaCard(album.title, album.artist,
                        onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) }, onPlay = { actions.playAlbum(album) },
                        artwork = { AlbumArtwork(album, it, MaterialTheme.shapes.extraSmall) }, modifier = Modifier.width(CardWidth),
                        tintKey = album.artworkData())
            }
        }
    }
}

private fun LazyListScope.playlistSection(playlists: List<Playlist>, actions: AppActions) {
    if (playlists.isEmpty()) return
    sectionHeader("Playlists", null)
    item(key = "row:playlists") {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { "${it.id}:${it.remoteId}" }) { playlist ->
                MediaCard(playlist.title, playlist.songCount?.let { songCount(it) } ?: "Playlist",
                        onClick = { actions.openPlaylist(playlist) }, onLongClick = { actions.playlistOptions(playlist) },
                        onPlay = { actions.playPlaylist(playlist) },
                        artwork = { modifier ->
                            PlaylistArtwork(playlist, modifier, MaterialTheme.shapes.extraSmall) { m ->
                                Box(m.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center) {
                                    Icon(playlist.icon, contentDescription = null, modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        },
                        modifier = Modifier.width(CardWidth), tintKey = playlist.coverArt?.let { CoverArt(it) })
            }
        }
    }
}
