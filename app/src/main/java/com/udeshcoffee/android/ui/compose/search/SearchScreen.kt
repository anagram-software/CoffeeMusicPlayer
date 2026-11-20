package com.udeshcoffee.android.ui.compose.search

import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumCard
import com.udeshcoffee.android.ui.compose.components.ArtistCard
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.SectionHeader
import com.udeshcoffee.android.ui.compose.components.SettingsButton
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.detail.SearchViewModel
import com.udeshcoffee.android.ui.compose.rememberCurrentSongId
import org.koin.compose.viewmodel.koinViewModel
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.AppActions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.udeshcoffee.android.data.subsonic.groupBySource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel = koinViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()

    Scaffold(topBar = {
        TopAppBar(
                actions = { SettingsButton() },
                title = {
                    TextField(
                            value = query,
                            onValueChange = { viewModel.query.value = it },
                            placeholder = { Text("Songs, albums, artists") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { viewModel.query.value = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                            },
                            colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth())
                })
    }) { padding ->
        val r = results
        when {
            r.searching -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
            r.error != null -> Text(r.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(padding).padding(16.dp))
            query.isNotBlank() && r.isEmpty -> EmptyState("No results for \"$query\"", Modifier.padding(padding), Icons.Default.Search)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                results(r.artists, r.albums, r.songs, currentId, actions)
            }
        }
    }
}

/**
 * Results grouped by source: the library, then each streaming provider behind Octo-Fiesta
 * (server only). With provider results, each group gets a heading, and tapping a provider song
 * plays just that song (streaming it adds it to the library); library songs play as a list.
 */
private fun LazyListScope.results(artists: List<Artist>, albums: List<Album>, songs: List<Song>, currentId: Long?, actions: AppActions) {
    val groups = groupBySource(artists, albums, songs)
    val mixed = groups.size > 1
    groups.forEach { group ->
        val key = group.provider ?: "library"
        if (mixed) item(key = "source:$key") {
            SourceHeader(
                    title = group.provider?.let { "From $it" } ?: "In your library",
                    subtitle = when {
                        group.provider != null -> "Not in your library yet. Playing a song adds it."
                        group.isEmpty -> "Nothing found in your library"
                        else -> null
                    },
                    icon = if (group.provider != null) Icons.Default.Cloud else Icons.Default.LibraryMusic)
        }
        if (group.artists.isNotEmpty()) {
            item(key = "$key:artists:header") { SectionHeader("Artists") }
            item(key = "$key:artists") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(group.artists, key = { it.id }) { artist ->
                        ArtistCard(artist, onClick = { actions.openArtist(artist) }, onLongClick = { actions.artistOptions(artist) },
                                onPlay = { actions.playArtist(artist) }, modifier = Modifier.width(140.dp))
                    }
                }
            }
        }
        if (group.albums.isNotEmpty()) {
            item(key = "$key:albums:header") { SectionHeader("Albums") }
            item(key = "$key:albums") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(group.albums, key = { it.id }) { album ->
                        AlbumCard(album, onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) },
                                onPlay = { actions.playAlbum(album) }, modifier = Modifier.width(140.dp))
                    }
                }
            }
        }
        // Provider playlists (Octo-Fiesta returns them as albums, which also open them).
        if (group.playlists.isNotEmpty()) {
            item(key = "$key:playlists:header") { SectionHeader("Playlists") }
            item(key = "$key:playlists") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(group.playlists, key = { it.id }) { playlist ->
                        AlbumCard(playlist, onClick = { actions.openAlbum(playlist) }, onLongClick = { actions.albumOptions(playlist) },
                                onPlay = { actions.playAlbum(playlist) }, modifier = Modifier.width(140.dp))
                    }
                }
            }
        }
        if (group.songs.isNotEmpty()) {
            item(key = "$key:songs:header") { SectionHeader("Songs") }
            items(group.songs, key = { "$key:song:${it.id}" }) { song ->
                SongRow(song,
                        onClick = { if (group.provider != null) actions.play(listOf(song)) else actions.play(group.songs, group.songs.indexOf(song)) },
                        onMore = { actions.songOptions(song) }, isCurrent = song.id == currentId)
            }
        }
    }
}

/** A source's heading ("In your library", "From Qobuz") above its artists, albums and songs. */
@Composable
private fun SourceHeader(title: String, subtitle: String?, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
