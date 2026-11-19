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
// remoteId / remote: the item is on the music server (Subsonic IDs; server genres go by name).
@Serializable data class AlbumRoute(val albumId: Long, val title: String, val remoteId: String? = null)
@Serializable data class ArtistRoute(val artistId: Long, val name: String, val remoteId: String? = null)
@Serializable data class GenreRoute(val genreId: Long, val name: String, val remote: Boolean = false)
@Serializable data class PlaylistRoute(val playlistId: Long, val type: Int, val title: String, val remoteId: String? = null, val readonly: Boolean = false)
@Serializable data class PlaylistAddSongsRoute(val playlistId: Long, val title: String)
@Serializable data class EditorRoute(val songId: Long)
