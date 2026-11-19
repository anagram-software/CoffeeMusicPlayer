package com.udeshcoffee.android.playback

import android.content.ContentUris
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.ServerAlbumSort
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.utils.ArtworkURI
import kotlinx.coroutines.flow.first

/**
 * Browse tree for media browsers such as Android Auto, for the current music source:
 * - device: root → Songs | Albums | Artists | Playlists → songs
 * - server: root → Random mix | Recently added (albums) | Artists | Playlists → songs
 * Server failures give empty lists rather than errors.
 */
class LibraryBrowser(
        private val mediaRepository: MediaRepository,
        private val subsonic: SubsonicRepository,
        private val sourceStore: MusicSourceStore
) {

    val root: MediaItem = browsable(ROOT, "Coffee Player", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    suspend fun children(parentId: String): List<MediaItem> =
            if (sourceStore.isServer) server { serverChildren(parentId) }.orEmpty() else deviceChildren(parentId)

    private suspend fun deviceChildren(parentId: String): List<MediaItem> = when {
        parentId == ROOT -> listOf(
                browsable(SONGS, "Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                browsable(ALBUMS, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsable(ARTISTS, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsable(PLAYLISTS, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
        parentId == SONGS -> mediaRepository.loadSongs().map { it.toMediaItem() }
        parentId == ALBUMS -> mediaRepository.loadAlbums().map { album ->
            browsable("$ALBUM_PREFIX${album.id}", album.title, MediaMetadata.MEDIA_TYPE_ALBUM, album.artist) {
                setArtworkUri(ContentUris.withAppendedId(ArtworkURI, album.id))
            }
        }
        parentId == ARTISTS -> mediaRepository.loadArtists().map { artist ->
            browsable("$ARTIST_PREFIX${artist.id}", artist.name, MediaMetadata.MEDIA_TYPE_ARTIST)
        }
        parentId == PLAYLISTS -> mediaRepository.observePlaylists().first().map { playlist ->
            browsable("$PLAYLIST_PREFIX${playlist.type}:${playlist.id}", playlist.title, MediaMetadata.MEDIA_TYPE_PLAYLIST)
        }
        parentId.startsWith(ALBUM_PREFIX) ->
            mediaRepository.loadAlbumSongs(parentId.removePrefix(ALBUM_PREFIX).toLong()).map { it.toMediaItem() }
        parentId.startsWith(ARTIST_PREFIX) ->
            mediaRepository.loadArtistSongs(parentId.removePrefix(ARTIST_PREFIX).toLong()).map { it.toMediaItem() }
        parentId.startsWith(PLAYLIST_PREFIX) -> {
            val (type, id) = parentId.removePrefix(PLAYLIST_PREFIX).split(":").map { it.toLong() }
            mediaRepository.loadPlaylistSongs(id, type.toInt()).map { it.toMediaItem() }
        }
        else -> emptyList()
    }

    // Server IDs are Subsonic IDs (strings), so they get their own prefixes.
    private suspend fun serverChildren(parentId: String): List<MediaItem> = when {
        parentId == ROOT -> listOf(
                browsable(SONGS, "Random mix", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                browsable(ALBUMS, "Recently added", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsable(ARTISTS, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsable(PLAYLISTS, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
        parentId == SONGS -> subsonic.randomSongs(100).map { it.toMediaItem() }
        parentId == ALBUMS -> subsonic.albums(ServerAlbumSort.NEWEST, offset = 0, count = 100).map { album ->
            browsable("$SERVER_ALBUM_PREFIX${album.remoteId}", album.title, MediaMetadata.MEDIA_TYPE_ALBUM, album.artist) {
                setArtworkUri(subsonic.coverArtUrl(album.coverArt ?: album.remoteId, 512)?.toUri())
            }
        }
        parentId == ARTISTS -> subsonic.artists().map { artist ->
            browsable("$SERVER_ARTIST_PREFIX${artist.remoteId}", artist.name, MediaMetadata.MEDIA_TYPE_ARTIST)
        }
        parentId == PLAYLISTS -> subsonic.playlists().map { playlist ->
            browsable("$SERVER_PLAYLIST_PREFIX${playlist.remoteId}", playlist.title, MediaMetadata.MEDIA_TYPE_PLAYLIST)
        }
        parentId.startsWith(SERVER_ALBUM_PREFIX) -> subsonic.albumSongs(parentId.removePrefix(SERVER_ALBUM_PREFIX)).map { it.toMediaItem() }
        parentId.startsWith(SERVER_ARTIST_PREFIX) -> subsonic.artistSongs(parentId.removePrefix(SERVER_ARTIST_PREFIX)).map { it.toMediaItem() }
        parentId.startsWith(SERVER_PLAYLIST_PREFIX) -> subsonic.playlistSongs(parentId.removePrefix(SERVER_PLAYLIST_PREFIX)).map { it.toMediaItem() }
        else -> emptyList()
    }

    /**
     * Songs matching a voice/search query ("play X"), title matches first, then artist and album.
     * An empty query means "play something": the whole library shuffled (a random mix on a server).
     */
    suspend fun search(query: String): List<MediaItem> {
        val q = query.trim()
        if (sourceStore.isServer) {
            val songs = server { if (q.isEmpty()) subsonic.randomSongs() else subsonic.search(q).songs }.orEmpty()
            return songs.map { it.toMediaItem() }
        }
        val songs = mediaRepository.loadSongs()
        if (q.isEmpty()) return songs.shuffled().map { it.toMediaItem() }
        val byTitle = songs.filter { it.title.contains(q, ignoreCase = true) }
        val byArtist = songs.filter { it.artistName.contains(q, ignoreCase = true) && it !in byTitle }
        val byAlbum = songs.filter { it.albumName.contains(q, ignoreCase = true) && it !in byTitle && it !in byArtist }
        return (byTitle + byArtist + byAlbum).map { it.toMediaItem() }
    }

    /** A playable item by media ID (a MediaStore song ID, or a server song's media ID), or null. */
    suspend fun song(mediaId: String): MediaItem? {
        if (mediaId.startsWith(SUBSONIC_MEDIA_PREFIX)) {
            if (!sourceStore.isServer) return null
            return subsonic.song(mediaId.removePrefix(SUBSONIC_MEDIA_PREFIX))?.toMediaItem()
        }
        if (sourceStore.isServer) return null
        val id = mediaId.toLongOrNull() ?: return null
        return mediaRepository.loadSongs().firstOrNull { it.id == id }?.toMediaItem()
    }

    private suspend fun <T> server(block: suspend () -> T): T? = try {
        block()
    } catch (e: SubsonicException) {
        Log.w(TAG, "Music server request failed", e)
        null
    }

    private fun browsable(id: String, title: String, type: Int, subtitle: String? = null,
                          extra: MediaMetadata.Builder.() -> Unit = {}): MediaItem =
            MediaItem.Builder()
                    .setMediaId(id)
                    .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(type)
                            .apply(extra)
                            .build())
                    .build()

    companion object {
        const val ROOT = "root"
        private const val TAG = "LibraryBrowser"
        private const val SONGS = "songs"
        private const val ALBUMS = "albums"
        private const val ARTISTS = "artists"
        private const val PLAYLISTS = "playlists"
        private const val ALBUM_PREFIX = "album:"
        private const val ARTIST_PREFIX = "artist:"
        private const val PLAYLIST_PREFIX = "playlist:"
        private const val SERVER_ALBUM_PREFIX = "server-album:"
        private const val SERVER_ARTIST_PREFIX = "server-artist:"
        private const val SERVER_PLAYLIST_PREFIX = "server-playlist:"
    }
}
