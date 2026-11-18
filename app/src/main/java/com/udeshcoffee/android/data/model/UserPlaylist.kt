package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created playlist. Stored in the app database instead of MediaStore, whose playlist
 * API is deprecated (API 31) and not writable under scoped storage.
 */
@Entity(tableName = "playlists")
data class UserPlaylist(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "date_added") val dateAdded: Long = System.currentTimeMillis()
)

/**
 * One entry of a [UserPlaylist]. [songId] is the MediaStore audio `_ID`; the same song may appear
 * more than once, so entries are identified by [id] (exposed as `Song.playlistSongId`).
 */
@Entity(
        tableName = "playlist_items",
        foreignKeys = [ForeignKey(
                entity = UserPlaylist::class,
                parentColumns = ["id"],
                childColumns = ["playlist_id"],
                onDelete = ForeignKey.CASCADE
        )],
        indices = [Index("playlist_id")]
)
data class PlaylistItem(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "playlist_id") val playlistId: Long,
        @ColumnInfo(name = "song_id") val songId: Long,
        @ColumnInfo(name = "position") val position: Int
)

/** Query result: a playlist with its entry count. */
data class UserPlaylistWithCount(
        @ColumnInfo(name = "id") val id: Long,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "song_count") val songCount: Int
)
