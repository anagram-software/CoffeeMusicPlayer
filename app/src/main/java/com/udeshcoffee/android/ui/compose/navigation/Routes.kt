package com.udeshcoffee.android.ui.compose.navigation

import kotlinx.serialization.Serializable

// Top-level destinations (navigation bar / rail).
@Serializable object HomeRoute
@Serializable object FavoritesRoute
@Serializable object LibraryRoute
@Serializable object SearchRoute

// Opened from the top app bar of the top-level screens.
@Serializable object SettingsRoute

// Detail screens.
/** A Library category's list; [category] is a [com.udeshcoffee.android.ui.compose.library.LibraryCategory] name. */
@Serializable data class LibraryCategoryRoute(val category: String)
// remoteId / remote: the item is on the music server (Subsonic IDs; server genres go by name).
/** [coverArt]: the server's cover art ID when known (it changes when the artwork does, unlike [remoteId]). */
@Serializable data class AlbumRoute(val albumId: Long, val title: String, val remoteId: String? = null, val coverArt: String? = null)
/** [coverArt]: as for [AlbumRoute]. */
@Serializable data class ArtistRoute(val artistId: Long, val name: String, val remoteId: String? = null, val coverArt: String? = null)
@Serializable data class GenreRoute(val genreId: Long, val name: String, val remote: Boolean = false)
/** [coverArt]: as for [AlbumRoute]. */
@Serializable data class PlaylistRoute(val playlistId: Long, val type: Int, val title: String, val remoteId: String? = null, val readonly: Boolean = false,
                                     val coverArt: String? = null)
@Serializable data class PlaylistAddSongsRoute(val playlistId: Long, val title: String)
@Serializable data class EditorRoute(val songId: Long)
