package com.udeshcoffee.android.data.subsonic

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song

/*
 * Octo-Fiesta (github.com/V1ck3s/octo-fiesta) is a Subsonic proxy that mixes streaming-provider
 * results (Qobuz, Deezer, Tidal...) into Navidrome's. Its items have IDs like "ext-qobuz-song-123",
 * and streaming one makes the proxy download it into the library. Besides search3, it also adds
 * provider items to library calls, found by searching the provider by name: getArtist appends the
 * provider's albums of a same-named artist, getAlbum appends tracks the library copy is missing,
 * and getTopSongs mixes in provider songs. Detail pages therefore show the library's items first
 * and the provider's separately. As in the web player (CoffeeMusicPlayerWeb `external.ts`,
 * `relevance.ts`).
 */

private val EXTERNAL_ID = Regex("^ext-([a-z0-9]+)-", RegexOption.IGNORE_CASE)

/**
 * A provider *playlist*, "pl-<provider>-<id>". search3 has no playlists, so Octo-Fiesta puts them
 * among the albums, with genre "Playlist" and the artist "🎵 <Provider> <owner>".
 */
private val PLAYLIST_ID = Regex("^pl-([a-z0-9]+)-", RegexOption.IGNORE_CASE)

/** True for a streaming-provider item served through Octo-Fiesta (not in the library yet), playlists included. */
fun isExternalId(id: String?): Boolean = id != null && (EXTERNAL_ID.containsMatchIn(id) || PLAYLIST_ID.containsMatchIn(id))

/** True for a provider playlist that Octo-Fiesta returns as a search3 album. */
fun isExternalPlaylistId(id: String?): Boolean = id != null && PLAYLIST_ID.containsMatchIn(id)

val Song.isExternal: Boolean get() = isExternalId(remoteId)
val Album.isExternal: Boolean get() = isExternalId(remoteId)
val Artist.isExternal: Boolean get() = isExternalId(remoteId)

private val providerNames = mapOf("qobuz" to "Qobuz", "deezer" to "Deezer", "tidal" to "Tidal", "yandex" to "Yandex", "squidwtf" to "SquidWTF")

/** Display name of the provider behind an external ID ("Qobuz"), or null for library items. */
fun externalProvider(id: String?): String? {
    val key = id?.let { EXTERNAL_ID.find(it) ?: PLAYLIST_ID.find(it) }?.groupValues?.get(1)?.lowercase() ?: return null
    return providerNames[key] ?: key.replaceFirstChar { it.uppercase() }
}

/**
 * Search results from one source: the library ([provider] null) or a streaming provider ("Qobuz").
 * [playlists] are the provider's playlists (still album objects, as Octo-Fiesta returns them; their
 * artist is the owner, see [playlistOwner]).
 */
data class SourceResults(val provider: String?, val artists: List<Artist>, val albums: List<Album>, val songs: List<Song>,
                         val playlists: List<Album> = emptyList()) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty() && playlists.isEmpty()
}

/**
 * Who made a provider playlist, from Octo-Fiesta's "🎵 Qobuz qobuz" artist: the owner after the
 * emoji and provider name, or the provider itself when the owner is the provider (or missing).
 */
fun playlistOwner(album: Album): String {
    val provider = externalProvider(album.remoteId) ?: return album.artist
    val owner = album.artist.dropWhile { !it.isLetterOrDigit() }.trim()
            .removePrefix(provider).trim().ifEmpty { provider }
    return if (owner.equals(provider, ignoreCase = true)) provider else owner
}

/**
 * Splits search results by source, keeping their order: the library first (always, even if
 * empty), then each provider in the order it first appears.
 */
fun groupBySource(artists: List<Artist>, albums: List<Album>, songs: List<Song>): List<SourceResults> {
    val providers = (artists.map { externalProvider(it.remoteId) } + albums.map { externalProvider(it.remoteId) } +
            songs.map { externalProvider(it.remoteId) }).filterNotNull().distinct()
    return (listOf<String?>(null) + providers).map { p ->
        val (playlists, sourceAlbums) = albums.filter { externalProvider(it.remoteId) == p }.partition { isExternalPlaylistId(it.remoteId) }
        SourceResults(p, artists.filter { externalProvider(it.remoteId) == p }, sourceAlbums, songs.filter { externalProvider(it.remoteId) == p },
                playlists.map { pl -> pl.copy(artist = playlistOwner(pl)).also { it.songCount = pl.songCount } })
    }
}

// ---- Relevance of provider results ----
// Providers carry no popularity data, and pad results for well-known music with filler.

private val COVER_PATTERNS = listOf(
        "\\bkaraoke\\b", "\\btribute\\b", "\\bmade famous\\b", "\\bin the style of\\b",
        "\\boriginally (performed|recorded|by)\\b", "\\blullab(y|ies)\\b", "\\brockabye baby\\b",
        "\\bvitamin string quartet\\b", "\\bbacking track\\b", "\\b8[- ]?bit\\b", "\\bsing[- ]?along\\b",
        "\\bcover version\\b", "\\binstrumental version\\b", "\\bworkout\\b", "\\bkidz? bop\\b",
).map { Regex(it) }

/** True if any of the texts (title, album, artist) look like a cover / karaoke / tribute release. */
fun looksLikeCover(vararg texts: String?): Boolean {
    val text = texts.filterNotNull().joinToString(" • ").lowercase()
    return COVER_PATTERNS.any { it.containsMatchIn(text) }
}

private val EDITION_TAG = Regex("[(\\[][^)\\]]*(deluxe|remaster|edition|explicit|expanded|anniversary|bonus|version|clean|mono|stereo|hi-?res)[^)\\]]*[)\\]]")
private val EDITION_SUFFIX = Regex("\\s+-\\s+(deluxe|remaster|edition|explicit).*$")

/** "Thriller (Deluxe Edition) [Remastered]" -> "thriller": for spotting editions of the same album. */
fun baseTitle(title: String): String =
        title.lowercase().replace(EDITION_TAG, "").replace(EDITION_SUFFIX, "").replace(Regex("\\s+"), " ").trim()

/** One album per base title, keeping the edition with the most tracks, in the original order. */
fun mergeEditions(albums: List<Album>): List<Album> {
    val best = HashMap<String, Album>()
    for (album in albums) {
        val key = baseTitle(album.title)
        val current = best[key]
        if (current == null || album.songCount > current.songCount) best[key] = album
    }
    val keep = best.values.toSet()
    return albums.filter { it in keep }
}

enum class ReleaseKind { ALBUM, SINGLE, APPEARS_ON }

/** Album (7+ tracks), single / EP (up to 6), or "appears on" (another artist's release, e.g. a compilation). */
fun releaseKind(album: Album, artistName: String): ReleaseKind {
    val by = album.artist.lowercase()
    if (by.isNotEmpty() && !by.contains(artistName.lowercase())) return ReleaseKind.APPEARS_ON
    return if (album.songCount in 1..6) ReleaseKind.SINGLE else ReleaseKind.ALBUM
}

/**
 * A server artist's releases: the library's grouped by kind, then the streaming provider's (behind
 * Octo-Fiesta), with cover / karaoke filler dropped ([hiddenCovers]) and duplicate editions merged.
 */
data class ArtistReleases(
        val albums: List<Album> = emptyList(),
        val singles: List<Album> = emptyList(),
        val appearsOn: List<Album> = emptyList(),
        /** Provider releases: albums, then singles, then appearances, each newest first. */
        val provider: List<Album> = emptyList(),
        val hiddenCovers: Int = 0,
) {
    val libraryCount: Int get() = albums.size + singles.size + appearsOn.size
    /** "Qobuz", for labelling [provider]. */
    val providerName: String? get() = provider.firstNotNullOfOrNull { externalProvider(it.remoteId) }
}

/** Groups getArtist's albums. [sortLibrary] orders each library group (the user's album sort). */
fun groupReleases(all: List<Album>, artistName: String, sortLibrary: (List<Album>) -> List<Album> = { it }): ArtistReleases {
    val (external, library) = all.partition { it.isExternal }
    val byKind = library.groupBy { releaseKind(it, artistName) }
    var albums = byKind[ReleaseKind.ALBUM].orEmpty()
    val singles = byKind[ReleaseKind.SINGLE].orEmpty()
    var appearsOn = byKind[ReleaseKind.APPEARS_ON].orEmpty()
    // Nothing credited to the artist directly (e.g. only compilations): show those as albums.
    if (albums.isEmpty() && singles.isEmpty()) {
        albums = appearsOn
        appearsOn = emptyList()
    }
    val kept = external.filter { !looksLikeCover(it.title, it.artist) }
    val provider = mergeEditions(kept)
            .sortedWith(compareBy<Album> { releaseKind(it, artistName).ordinal }.thenByDescending { it.year })
    return ArtistReleases(sortLibrary(albums), sortLibrary(singles), sortLibrary(appearsOn), provider, external.size - kept.size)
}

/** An album's tracks: the library's, and the ones Octo-Fiesta adds from the provider. */
data class AlbumTracks(val library: List<Song>, val provider: List<Song>) {
    /** What playing the album plays: its library tracks, or everything for an album that is entirely the provider's. */
    val playable: List<Song> get() = library.ifEmpty { provider }
}

fun splitAlbumTracks(songs: List<Song>): AlbumTracks {
    val (provider, library) = songs.partition { it.isExternal }
    return AlbumTracks(library, provider)
}

/**
 * The provider album to star for "Add to library": the album itself if it's the provider's,
 * else the provider album its missing tracks come from. Null if neither is known.
 */
fun externalAlbumId(albumId: String, missing: List<Song>): String? =
        albumId.takeIf { isExternalId(it) && !isExternalPlaylistId(it) } ?: missing.firstNotNullOfOrNull { it.albumRemoteId?.takeIf(::isExternalId) }
