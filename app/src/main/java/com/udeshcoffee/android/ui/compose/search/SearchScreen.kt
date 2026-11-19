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

private fun LazyListScope.results(artists: List<Artist>, albums: List<Album>, songs: List<Song>, currentId: Long?, actions: AppActions) {
    if (artists.isNotEmpty()) {
        item { SectionHeader("Artists") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(artists, key = { it.id }) { artist ->
                    ArtistCard(artist, onClick = { actions.openArtist(artist) }, onLongClick = { actions.artistOptions(artist) },
                            onPlay = { actions.playArtist(artist) }, modifier = Modifier.width(140.dp), collectImage = false)
                }
            }
        }
    }
    if (albums.isNotEmpty()) {
        item { SectionHeader("Albums") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album, onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) },
                            onPlay = { actions.playAlbum(album) }, modifier = Modifier.width(140.dp))
                }
            }
        }
    }
    if (songs.isNotEmpty()) {
        item { SectionHeader("Songs") }
        items(songs, key = { it.id }) { song ->
            SongRow(song, onClick = { actions.play(songs, songs.indexOf(song)) }, onMore = { actions.songOptions(song) },
                    isCurrent = song.id == currentId)
        }
    }
}
