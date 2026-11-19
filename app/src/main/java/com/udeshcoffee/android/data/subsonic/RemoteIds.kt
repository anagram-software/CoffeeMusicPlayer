package com.udeshcoffee.android.data.subsonic

/**
 * Server items have string IDs, but the app keys songs, albums and artists by Long (MediaStore
 * IDs, which are positive). Server items get a stable negative Long derived from their string ID,
 * so both kinds can share lists, queues and navigation without colliding.
 */
object RemoteIds {

    /** A negative 63-bit FNV-1a hash of [kind] and [remoteId]. */
    fun toLong(kind: String, remoteId: String): Long {
        var hash = -0x340d631b7bdddcdbL // FNV-1a 64-bit offset basis
        for (b in "$kind:$remoteId".toByteArray()) {
            hash = hash xor (b.toLong() and 0xff)
            hash *= 0x100000001b3L
        }
        return -(hash and Long.MAX_VALUE) - 1
    }

    fun song(remoteId: String) = toLong("song", remoteId)
    fun album(remoteId: String) = toLong("album", remoteId)
    fun artist(remoteId: String) = toLong("artist", remoteId)
    fun genre(name: String) = toLong("genre", name)
    fun playlist(remoteId: String) = toLong("playlist", remoteId)
}
