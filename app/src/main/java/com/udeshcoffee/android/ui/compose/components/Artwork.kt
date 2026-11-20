package com.udeshcoffee.android.ui.compose.components

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
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
import com.udeshcoffee.android.model.Playlist
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.NewReleases
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.artistImageFile
import com.udeshcoffee.android.utils.artworkData
import com.udeshcoffee.android.utils.CoverArt

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

/** Icon for a playlist: the smart playlists have their own, user playlists a generic one. */
val Playlist.icon: ImageVector
    get() = when (type) {
        Playlist.MOST_PLAYED -> Icons.AutoMirrored.Filled.TrendingUp
        Playlist.RECENTLY_PLAYED -> Icons.Default.History
        Playlist.RECENTLY_ADDED -> Icons.Default.NewReleases
        Playlist.DOWNLOADS -> Icons.Default.DownloadDone
        else -> Icons.AutoMirrored.Filled.PlaylistPlay
    }

/**
 * A playlist's picture: the server's cover art for server playlists (with the playlist icon while it
 * loads or if it fails), else [noArtwork], since device playlists have none.
 */
@Composable
fun PlaylistArtwork(playlist: Playlist, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small,
                    noArtwork: @Composable (Modifier) -> Unit) {
    val cover = playlist.coverArt
    if (cover != null) ArtworkImage(CoverArt(cover), playlist.icon, modifier, shape) else noArtwork(modifier)
}

@Composable
fun SongArtwork(song: Song?, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        ArtworkImage(song?.artworkData(), Icons.Default.MusicNote, modifier, shape)

@Composable
fun AlbumArtwork(albumId: Long, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        ArtworkImage(AlbumArt(albumId), Icons.Default.Album, modifier, shape)

@Composable
fun AlbumArtwork(album: Album, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) =
        ArtworkImage(album.artworkData(), Icons.Default.Album, modifier, shape)

/**
 * Artist image. Server artists use the server's image; device artists the image the user chose
 * (stored in app storage), if any.
 */
@Composable
fun ArtistArtwork(artist: Artist, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    if (artist.isRemote) {
        ArtworkImage((artist.coverArt ?: artist.remoteId)?.let { CoverArt(it) }, Icons.Default.Person, modifier, shape)
        return
    }
    val context = LocalContext.current
    // File checks run off the main thread; the cache key changes whenever the image file does.
    val model by produceState<Any?>(null, artist.id) {
        suspend fun stored(): Any? = withContext(Dispatchers.IO) {
            val file = artistImageFile(artist.id)
            if (!file.exists()) null
            else coil3.request.ImageRequest.Builder(context).data(file)
                    .memoryCacheKey("artist:${file.path}:${file.lastModified()}").build()
        }
        value = stored()
    }
    ArtworkImage(model, Icons.Default.Person, modifier, shape)
}
