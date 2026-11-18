package com.udeshcoffee.android.playback

import android.content.ContentUris
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.utils.ArtworkURI

/**
 * Browse tree for media browsers such as Android Auto:
 * root → Songs | Albums | Artists | Playlists → songs.
 * All calls block on MediaStore/Room; run them off the main thread.
 */
class LibraryBrowser(private val mediaRepository: MediaRepository) {

    val root: MediaItem = browsable(ROOT, "Coffee Player", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    fun children(parentId: String): List<MediaItem> = when {
        parentId == ROOT -> listOf(
                browsable(SONGS, "Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                browsable(ALBUMS, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsable(ARTISTS, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsable(PLAYLISTS, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
        parentId == SONGS -> mediaRepository.getSongs().blockingFirst().map { it.toMediaItem() }
        parentId == ALBUMS -> mediaRepository.getAlbums().blockingFirst().map { album ->
            browsable("$ALBUM_PREFIX${album.id}", album.title, MediaMetadata.MEDIA_TYPE_ALBUM, album.artist) {
                setArtworkUri(ContentUris.withAppendedId(ArtworkURI, album.id))
            }
        }
        parentId == ARTISTS -> mediaRepository.getArtists().blockingFirst().map { artist ->
            browsable("$ARTIST_PREFIX${artist.id}", artist.name, MediaMetadata.MEDIA_TYPE_ARTIST)
        }
        parentId == PLAYLISTS -> mediaRepository.getPlaylists().blockingFirst().map { playlist ->
            browsable("$PLAYLIST_PREFIX${playlist.type}:${playlist.id}", playlist.title, MediaMetadata.MEDIA_TYPE_PLAYLIST)
        }
        parentId.startsWith(ALBUM_PREFIX) ->
            mediaRepository.getAlbumSongs(parentId.removePrefix(ALBUM_PREFIX).toLong()).blockingFirst().map { it.toMediaItem() }
        parentId.startsWith(ARTIST_PREFIX) ->
            mediaRepository.getArtistSongs(parentId.removePrefix(ARTIST_PREFIX).toLong()).blockingFirst().map { it.toMediaItem() }
        parentId.startsWith(PLAYLIST_PREFIX) -> {
            val (type, id) = parentId.removePrefix(PLAYLIST_PREFIX).split(":").map { it.toLong() }
            mediaRepository.getPlaylistSongs(id, type.toInt())?.blockingFirst()?.map { it.toMediaItem() } ?: emptyList()
        }
        else -> emptyList()
    }

    /**
     * Songs matching a voice/search query ("play X"), title matches first, then artist and album.
     * An empty query means "play something": the whole library, shuffled.
     */
    fun search(query: String): List<MediaItem> {
        val songs = mediaRepository.getSongs().blockingFirst()
        val q = query.trim()
        if (q.isEmpty()) return songs.shuffled().map { it.toMediaItem() }
        val byTitle = songs.filter { it.title.contains(q, ignoreCase = true) }
        val byArtist = songs.filter { it.artistName.contains(q, ignoreCase = true) && it !in byTitle }
        val byAlbum = songs.filter { it.albumName.contains(q, ignoreCase = true) && it !in byTitle && it !in byArtist }
        return (byTitle + byArtist + byAlbum).map { it.toMediaItem() }
    }

    /** A playable item by media ID (a MediaStore song ID), or null. */
    fun song(mediaId: String): MediaItem? {
        val id = mediaId.toLongOrNull() ?: return null
        return mediaRepository.getSongs().blockingFirst().firstOrNull { it.id == id }?.toMediaItem()
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
        private const val SONGS = "songs"
        private const val ALBUMS = "albums"
        private const val ARTISTS = "artists"
        private const val PLAYLISTS = "playlists"
        private const val ALBUM_PREFIX = "album:"
        private const val ARTIST_PREFIX = "artist:"
        private const val PLAYLIST_PREFIX = "playlist:"
    }
}
