package com.udeshcoffee.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.udeshcoffee.android.data.model.PlaylistItem
import com.udeshcoffee.android.data.model.UserPlaylist
import com.udeshcoffee.android.data.model.UserPlaylistWithCount
import io.reactivex.Flowable

@Dao
abstract class PlaylistDao {

    @Query("""SELECT p.id AS id, p.name AS name, COUNT(i.id) AS song_count
              FROM playlists p LEFT JOIN playlist_items i ON i.playlist_id = p.id
              GROUP BY p.id ORDER BY p.name COLLATE NOCASE""")
    abstract fun observePlaylists(): Flowable<List<UserPlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id != :exceptId ORDER BY name COLLATE NOCASE")
    abstract fun getPlaylistsExcept(exceptId: Long): List<UserPlaylist>

    @Query("SELECT * FROM playlist_items WHERE playlist_id = :playlistId ORDER BY position")
    abstract fun observeItems(playlistId: Long): Flowable<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE playlist_id = :playlistId ORDER BY position")
    abstract fun getItems(playlistId: Long): List<PlaylistItem>

    @Query("SELECT song_id FROM playlist_items WHERE playlist_id = :playlistId")
    abstract fun getSongIds(playlistId: Long): List<Long>

    @Insert
    abstract fun insertPlaylist(playlist: UserPlaylist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    abstract fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    protected abstract fun deletePlaylistRow(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId")
    protected abstract fun deleteAllItems(playlistId: Long)

    @Transaction
    open fun deletePlaylist(playlistId: Long) {
        deleteAllItems(playlistId)
        deletePlaylistRow(playlistId)
    }

    @Insert
    protected abstract fun insertItems(items: List<PlaylistItem>)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_items WHERE playlist_id = :playlistId")
    protected abstract fun nextPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    protected abstract fun deleteItem(itemId: Long)

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId AND song_id = :songId")
    abstract fun removeSong(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_items SET position = :position WHERE id = :itemId")
    protected abstract fun setPosition(itemId: Long, position: Int)

    @Transaction
    open fun appendSongs(playlistId: Long, songIds: List<Long>) {
        val start = nextPosition(playlistId)
        insertItems(songIds.mapIndexed { i, songId -> PlaylistItem(playlistId = playlistId, songId = songId, position = start + i) })
    }

    @Transaction
    open fun removeItem(playlistId: Long, itemId: Long) {
        deleteItem(itemId)
        renumber(getItems(playlistId))
    }

    /** Moves the entry at index [from] to index [to] (indices in play order). */
    @Transaction
    open fun moveItem(playlistId: Long, from: Int, to: Int) {
        val items = getItems(playlistId).toMutableList()
        if (from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
        renumber(items)
    }

    @Transaction
    open fun createWithSongs(name: String, songIds: List<Long>): Long {
        val id = insertPlaylist(UserPlaylist(name = name))
        appendSongs(id, songIds)
        return id
    }

    private fun renumber(items: List<PlaylistItem>) {
        items.forEachIndexed { index, item -> if (item.position != index) setPosition(item.id, index) }
    }
}
