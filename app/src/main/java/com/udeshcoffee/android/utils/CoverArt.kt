package com.udeshcoffee.android.utils

import coil3.EventListener
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.map.Mapper
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.Options
import coil3.request.SuccessResult
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** Coil model for music server artwork ([id] is a Subsonic cover art ID). */
data class CoverArt(val id: String, val size: Int = 600)

/**
 * Turns [CoverArt] into the signed-in server's getCoverArt URL (nothing when signed out). The URL
 * carries the auth token, so [CoverArtCache.Keys] keys the caches by the model instead.
 */
class CoverArtMapper(private val subsonic: () -> SubsonicRepository) : Mapper<CoverArt, String> {
    override fun map(data: CoverArt, options: Options): String? = subsonic().coverArtUrl(data.id, data.size)
}

/**
 * Local cache of server artwork that can show an outdated picture while the current one loads.
 *
 * Each cover is cached (memory and disk) under `coverart:<id>:<size>`, without the token. Two kinds
 * of cover art ID:
 * - **Versioned** (Navidrome: `al-<album>_<updated hex>`): the ID changes when the artwork does, so a
 *   cached picture is never out of date. When one comes from the network it's also copied to its
 *   unversioned key ([latestId], `al-<album>`), so after a change the previous picture shows until
 *   the new ID loads.
 * - **Unversioned** (other servers, Octo-Fiesta's `ext-…`): the picture behind an ID can change, so
 *   the cached one shows at once and is fetched again from the server once per app run, then replaced.
 */
object CoverArtCache {

    private val NAVIDROME_VERSIONED = Regex("^([a-z]{2}-[^_]+)_[0-9a-f]+$")

    /** The unversioned form of a versioned cover art [id], or null when [id] isn't versioned. */
    fun latestId(id: String): String? = NAVIDROME_VERSIONED.matchEntire(id)?.groupValues?.get(1)

    fun key(id: String, size: Int): String = "coverart:$id:$size"

    fun key(data: CoverArt): String = key(data.id, data.size)

    /** Where the newest picture of [data]'s item is cached: its own key, or its unversioned one. */
    fun latestKey(data: CoverArt): String = key(latestId(data.id) ?: data.id, data.size)

    /** Unversioned IDs fetched (or tried) from the server in this app run. */
    private val revalidated = ConcurrentHashMap.newKeySet<String>()

    /** Whether [data] should be fetched from the server even though it may be cached. */
    fun needsRevalidation(data: CoverArt): Boolean = latestId(data.id) == null && key(data) !in revalidated

    fun markRevalidated(data: CoverArt) {
        revalidated += key(data)
    }

    /** Keys [CoverArt] requests' memory and disk cache entries by [key] (unless a request sets its own). */
    class Keys : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val data = request.data as? CoverArt ?: return chain.proceed()
            if (request.memoryCacheKey != null && request.diskCacheKey != null) return chain.proceed()
            val key = key(data)
            val keyed = request.newBuilder()
                    .apply { if (request.memoryCacheKey == null) memoryCacheKey(key) }
                    .apply { if (request.diskCacheKey == null) diskCacheKey(key) }
                    .build()
            return chain.withRequest(keyed).proceed()
        }
    }

    /** Copies versioned covers fetched from the network to their [latestKey] on disk. */
    class KeepLatest : EventListener() {
        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            val data = request.data as? CoverArt ?: return
            val source = result.diskCacheKey ?: return
            if (result.dataSource != DataSource.NETWORK || latestId(data.id) == null) return
            val target = latestKey(data)
            if (source == target) return
            ProcessLifecycleScope.launch(Dispatchers.IO) {
                val disk = SingletonImageLoader.get(request.context).diskCache ?: return@launch
                runCatching {
                    disk.openSnapshot(source)?.use { snapshot ->
                        val editor = disk.openEditor(target) ?: return@use
                        try {
                            disk.fileSystem.copy(snapshot.metadata, editor.metadata)
                            disk.fileSystem.copy(snapshot.data, editor.data)
                            editor.commit()
                        } catch (e: Exception) {
                            editor.abort()
                        }
                    }
                }
            }
        }
    }
}

/** Artwork model for a song: MediaStore album art, or server cover art. */
fun Song.artworkData(): Any? =
        if (isRemote) (coverArt ?: albumRemoteId)?.let { CoverArt(it) } else AlbumArt(albumId, dateModified)

/** Artwork model for an album. */
fun Album.artworkData(): Any? =
        if (isRemote) (coverArt ?: remoteId)?.let { CoverArt(it) } else AlbumArt(id, dateModified)
