package com.udeshcoffee.android.ui.compose.components

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.artistImageFile
import com.udeshcoffee.android.utils.collectArtistImage

/** Image with a tinted icon placeholder shown until (or instead of) the image. */
@Composable
fun ArtworkImage(model: Any?, fallback: ImageVector, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    var loaded by remember(model) { mutableStateOf(false) }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (!loaded) {
            Icon(fallback, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize().padding(12.dp))
        }
        AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun SongArtwork(song: Song?, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        ArtworkImage(song?.let { AlbumArt(it.albumId, it.dateModified) }, Icons.Default.MusicNote, modifier, shape)

@Composable
fun AlbumArtwork(albumId: Long, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        ArtworkImage(AlbumArt(albumId), Icons.Default.Album, modifier, shape)

@Composable
fun AlbumArtwork(album: Album, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        AlbumArtwork(album.id, modifier, shape)

/** Artist image from app storage; downloads it from Last.fm first when [collect] is set (and settings allow). */
@Composable
fun ArtistArtwork(artist: Artist, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small, collect: Boolean = false) {
    val context = LocalContext.current
    var version by remember(artist.id) { mutableIntStateOf(0) }
    val file = artistImageFile(artist.id)
    androidx.compose.runtime.LaunchedEffect(artist.id, collect) {
        if (collect && !file.exists() && artist.name != "<unknown>" && collectArtistImage(context, artist.id, artist.name)) version++
    }
    val model = if (file.exists()) coil3.request.ImageRequest.Builder(context).data(file)
            .memoryCacheKey("artist:${file.path}:${file.lastModified()}:$version").build() else null
    ArtworkImage(model, Icons.Default.Person, modifier, shape)
}
