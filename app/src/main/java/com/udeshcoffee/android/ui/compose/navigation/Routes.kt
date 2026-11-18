package com.udeshcoffee.android.ui.compose.navigation

import kotlinx.serialization.Serializable

// Top-level destinations (navigation bar / rail).
@Serializable object LibraryRoute
@Serializable object FavoritesRoute
@Serializable object PlaylistsRoute
@Serializable object EqualizerRoute
@Serializable object SettingsRoute

// Detail screens.
@Serializable data class AlbumRoute(val albumId: Long, val title: String)
@Serializable data class ArtistRoute(val artistId: Long, val name: String)
@Serializable data class GenreRoute(val genreId: Long, val name: String)
@Serializable data class PlaylistRoute(val playlistId: Long, val type: Int, val title: String)
@Serializable data class PlaylistAddSongsRoute(val playlistId: Long, val title: String)
@Serializable object SearchRoute
@Serializable data class EditorRoute(val songId: Long)
