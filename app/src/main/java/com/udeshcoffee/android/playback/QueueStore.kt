package com.udeshcoffee.android.playback

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.Permissions
import com.udeshcoffee.android.utils.PreferenceUtil

/**
 * Persists the play queue so it survives process death. Uses the same preference keys and
 * encoding as earlier versions, so queues saved by older releases still restore.
 */
class QueueStore(private val context: Context, private val mediaRepository: MediaRepository) {

    data class SavedQueue(val songs: List<Song>, val index: Int, val positionMs: Long)

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun save(songIds: List<Long>, index: Int, positionMs: Long) {
        prefs.edit {
            putString(PreferenceUtil.LAST_QUEUE, encode(songIds))
            putInt(PreferenceUtil.LAST_PLAYED_POS, index)
            putInt(PreferenceUtil.LAST_CURRENT_POS, positionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        }
    }

    /** Loads the saved queue, resolving IDs against MediaStore. Blocking; call off the main thread. */
    fun load(): SavedQueue? {
        if (!Permissions.hasReadAudio(context)) return null
        val ids = decode(prefs.getString(PreferenceUtil.LAST_QUEUE, "") ?: "")
        if (ids.isEmpty()) return null
        val byId = mediaRepository.getSongs().blockingFirst().associateBy { it.id }
        val songs = ids.mapNotNull { byId[it] }
        if (songs.isEmpty()) return null
        val index = prefs.getInt(PreferenceUtil.LAST_PLAYED_POS, 0).coerceIn(0, songs.size - 1)
        return SavedQueue(songs, index, prefs.getInt(PreferenceUtil.LAST_CURRENT_POS, 0).toLong())
    }

    companion object {
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
