package com.udeshcoffee.android.data

import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.PlaylistPlays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** An album or playlist in a "Recently played" list (Home and Library). */
sealed interface RecentItem {
    /** When it was last played (epoch millis), or 0 if unknown. */
    val playedAt: Long
    /** A stable list key, distinct between albums and playlists. */
    val key: String

    data class OfAlbum(val album: Album, override val playedAt: Long) : RecentItem {
        override val key: String get() = "album:${album.remoteId ?: album.id}"
    }

    data class OfPlaylist(val playlist: Playlist, override val playedAt: Long) : RecentItem {
        override val key: String get() = "playlist:${PlaylistPlays.key(playlist)}"
    }
}

/** Device: the albums of [songs] (recently played first), each played when its latest song was. */
fun recentAlbums(songs: List<Song>): List<RecentItem.OfAlbum> {
    val latest = HashMap<Long, Long>()
    for (song in songs) latest.merge(song.albumId, song.lastplayed * 1000L, ::maxOf)
    return songsToAlbums(songs).map { RecentItem.OfAlbum(it, latest[it.id] ?: 0) }
}

/**
 * [albums] and the [playlists] that have been played from this app ([plays], by [PlaylistPlays.key]),
 * most recent first, at most [count]. Unknown album times sort last, in their original order.
 */
fun mergeRecent(albums: List<RecentItem.OfAlbum>, playlists: List<Playlist>, plays: Map<String, Long>, count: Int): List<RecentItem> {
    val played = playlists.mapNotNull { p -> plays[PlaylistPlays.key(p)]?.let { RecentItem.OfPlaylist(p, it) } }
    return (albums + played).sortedByDescending { it.playedAt }.take(count)
}

/**
 * Device "Recently played": albums from the app's play stats and the playlists played in the app
 * (the Recently Played smart playlist itself left out). Call [PlaylistPlays.load] (off the main thread) to start it.
 */
fun MediaRepository.observeRecentlyPlayed(count: Int): Flow<List<RecentItem>> = combine(
        observePlaylistSongs(0, Playlist.RECENTLY_PLAYED).map(::recentAlbums),
        observePlaylists().map { all -> all.filter { it.type != Playlist.RECENTLY_PLAYED } },
        PlaylistPlays.plays.filterNotNull()
) { albums, playlists, plays -> mergeRecent(albums, playlists, plays, count) }
