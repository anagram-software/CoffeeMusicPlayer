package com.udeshcoffee.android.playback

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.source.MusicSource
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.ServerSong
import com.udeshcoffee.android.data.subsonic.toServerSong
import com.udeshcoffee.android.data.subsonic.toSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.Permissions
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists the play queue so it survives process death. Device songs are stored by MediaStore ID
 * and looked up again; music server songs are stored with their details, so restoring needs no
 * network (so they can later restore offline too). Only entries from the current [MusicSource]
 * restore.
 */
class QueueStore(
        private val context: Context,
        private val mediaRepository: MediaRepository,
        private val sourceStore: MusicSourceStore
) {

    data class SavedQueue(val songs: List<Song>, val index: Int, val positionMs: Long)

    /** One queue entry: a device song ID or a server song. */
    @Serializable
    data class Entry(val localId: Long? = null, val remote: ServerSong? = null) {
        fun belongsTo(source: MusicSource): Boolean = when (source) {
            MusicSource.DEVICE -> localId != null
            MusicSource.SERVER -> remote != null
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun save(songs: List<Song>, index: Int, positionMs: Long) {
        prefs.edit {
            putString(KEY_QUEUE, encodeEntries(songs))
            putInt(PreferenceUtil.LAST_PLAYED_POS, index)
            putInt(PreferenceUtil.LAST_CURRENT_POS, positionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_QUEUE)
            remove(PreferenceUtil.LAST_PLAYED_POS)
            remove(PreferenceUtil.LAST_CURRENT_POS)
        }
    }

    /**
     * Loads the saved queue. Entries from the other [MusicSource] and device songs that are gone (or
     * can't be read without the permission) are dropped; the current song stays current if it survives.
     */
    suspend fun load(): SavedQueue? {
        if (!sourceStore.isConfigured) return null
        val source = sourceStore.current
        val entries = prefs.getString(KEY_QUEUE, null)?.let { decodeEntries(it) } ?: return null
        if (entries.none { it.belongsTo(source) }) return null
        val local = if (source == MusicSource.DEVICE && Permissions.hasReadAudio(context))
            mediaRepository.loadSongs().associateBy { it.id } else emptyMap()
        val savedIndex = prefs.getInt(PreferenceUtil.LAST_PLAYED_POS, 0)
        val songs = ArrayList<Song>(entries.size)
        var index = 0
        var currentSurvived = false
        entries.forEachIndexed { i, entry ->
            if (!entry.belongsTo(source)) return@forEachIndexed
            val song = entry.localId?.let { local[it] } ?: entry.remote?.toSong() ?: return@forEachIndexed
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
        private const val KEY_QUEUE = "queue"

        private val json = Json { ignoreUnknownKeys = true }

        fun encodeEntries(songs: List<Song>): String = json.encodeToString(ListSerializer(Entry.serializer()), songs.map { song ->
            song.toServerSong()?.let { Entry(remote = it) } ?: Entry(localId = song.id)
        })

        fun decodeEntries(text: String): List<Entry> =
                runCatching { json.decodeFromString(ListSerializer(Entry.serializer()), text) }.getOrDefault(emptyList())
    }
}
