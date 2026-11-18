package com.udeshcoffee.android.playback

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.subsonic.RemoteIds
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.Permissions
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists the play queue so it survives process death. Device songs are stored by MediaStore ID
 * and looked up again; music server songs are stored with their details, so restoring needs no
 * network. Queues saved by older releases (the reverse-hex [LAST_QUEUE] format) still restore.
 */
class QueueStore(
        private val context: Context,
        private val mediaRepository: MediaRepository,
        private val subsonic: SubsonicRepository
) {

    data class SavedQueue(val songs: List<Song>, val index: Int, val positionMs: Long)

    /** One queue entry: a device song ID or a server song. */
    @Serializable
    data class Entry(val localId: Long? = null, val remote: RemoteSong? = null)

    @Serializable
    data class RemoteSong(
            val id: String, val title: String, val album: String, val albumId: String? = null,
            val artist: String, val artistId: String? = null, val coverArt: String? = null,
            val durationMs: Long = 0, val track: Int = 0, val disc: Int = 0, val year: Int = 0
    )

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun save(songs: List<Song>, index: Int, positionMs: Long) {
        prefs.edit {
            putString(KEY_QUEUE, encodeEntries(songs))
            remove(PreferenceUtil.LAST_QUEUE)
            putInt(PreferenceUtil.LAST_PLAYED_POS, index)
            putInt(PreferenceUtil.LAST_CURRENT_POS, positionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        }
    }

    /**
     * Loads the saved queue. Device songs that are gone (or can't be read without the permission)
     * and server songs while signed out are dropped; the current song stays current if it survives.
     */
    suspend fun load(): SavedQueue? {
        val entries = prefs.getString(KEY_QUEUE, null)?.let { decodeEntries(it) }
                ?: decode(prefs.getString(PreferenceUtil.LAST_QUEUE, "") ?: "").map { Entry(localId = it) }
        if (entries.isEmpty()) return null
        val local = if (entries.any { it.localId != null } && Permissions.hasReadAudio(context))
            mediaRepository.loadSongs().associateBy { it.id } else emptyMap()
        val savedIndex = prefs.getInt(PreferenceUtil.LAST_PLAYED_POS, 0)
        val songs = ArrayList<Song>(entries.size)
        var index = 0
        var currentSurvived = false
        entries.forEachIndexed { i, entry ->
            val song = entry.localId?.let { local[it] } ?: entry.remote?.takeIf { subsonic.isSignedIn }?.toSong()
                    ?: return@forEachIndexed
            // The saved current song, or else the nearest one before it.
            if (i <= savedIndex) {
                index = songs.size
                currentSurvived = i == savedIndex
            }
            songs.add(song)
        }
        if (songs.isEmpty()) return null
        val position = if (currentSurvived) prefs.getInt(PreferenceUtil.LAST_CURRENT_POS, 0).toLong() else 0L
        return SavedQueue(songs, index, position)
    }

    companion object {
        private const val KEY_QUEUE = "queue_v2"
        private const val HEX = "0123456789abcdef"

        /** "Reverse hexadecimal": least significant digit first, each ID terminated by ';'. */
        fun encode(ids: List<Long>): String = buildString {
            for (id in ids) {
                if (id < 0) continue
                var n = id
                if (n == 0L) append('0')
                while (n != 0L) {
                    append(HEX[(n and 0xf).toInt()])
                    n = n ushr 4
                }
                append(';')
            }
        }

        private val json = Json { ignoreUnknownKeys = true }

        fun encodeEntries(songs: List<Song>): String = json.encodeToString(ListSerializer(Entry.serializer()), songs.map { song ->
            val remote = song.remoteId
            if (remote == null) Entry(localId = song.id)
            else Entry(remote = RemoteSong(remote, song.title, song.albumName, song.albumRemoteId, song.artistName,
                    song.artistRemoteId, song.coverArt, song.duration, song.track, song.discNumber, song.year))
        })

        fun decodeEntries(text: String): List<Entry> =
                runCatching { json.decodeFromString(ListSerializer(Entry.serializer()), text) }.getOrDefault(emptyList())

        private fun RemoteSong.toSong() = Song(RemoteIds.song(id), title, albumId?.let { RemoteIds.album(it) } ?: 0, album,
                artistId?.let { RemoteIds.artist(it) } ?: 0, artist, durationMs, year, 0, 0, disc * 1000 + track,
                remoteId = id, coverArt = coverArt, albumRemoteId = albumId, artistRemoteId = artistId)

        fun decode(q: String): List<Long> {
            val ids = ArrayList<Long>()
            var n = 0L
            var shift = 0
            for (c in q) {
                when (c) {
                    ';' -> { ids.add(n); n = 0; shift = 0 }
                    in '0'..'9' -> { n += (c - '0').toLong() shl shift; shift += 4 }
                    in 'a'..'f' -> { n += (10 + (c - 'a')).toLong() shl shift; shift += 4 }
                }
            }
            return ids
        }
    }
}
