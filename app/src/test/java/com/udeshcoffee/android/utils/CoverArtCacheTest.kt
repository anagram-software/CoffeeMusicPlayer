package com.udeshcoffee.android.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverArtCacheTest {

    @Test
    fun `navidrome cover art IDs are versioned`() {
        assertEquals("al-al1", CoverArtCache.latestId("al-al1_abc"))
        assertEquals("ar-5mBKXuA3oZrzcA4j1qvxpt", CoverArtCache.latestId("ar-5mBKXuA3oZrzcA4j1qvxpt_67a1b2c3"))
        assertEquals("mf-s1", CoverArtCache.latestId("mf-s1_0"))
        assertEquals("pl-p1", CoverArtCache.latestId("pl-p1_6650f00d"))
    }

    @Test
    fun `other cover art IDs are not versioned`() {
        assertNull(CoverArtCache.latestId("mf-s1"))
        assertNull(CoverArtCache.latestId("al-123"))
        assertNull(CoverArtCache.latestId("ext-deezer-album-12345"))
        assertNull(CoverArtCache.latestId("pl-qobuz-987"))
        assertNull(CoverArtCache.latestId("al-a1_notHex"))
        assertNull(CoverArtCache.latestId("1234"))
    }

    @Test
    fun `versioned covers keep their newest picture under the unversioned key`() {
        assertEquals("coverart:al-al1_abc:600", CoverArtCache.key(CoverArt("al-al1_abc")))
        assertEquals("coverart:al-al1:600", CoverArtCache.latestKey(CoverArt("al-al1_abc")))
        assertEquals("coverart:ext-deezer-album-1:300", CoverArtCache.latestKey(CoverArt("ext-deezer-album-1", 300)))
    }

    @Test
    fun `only unversioned covers are fetched again, once`() {
        assertFalse(CoverArtCache.needsRevalidation(CoverArt("al-al1_abc")))
        val cover = CoverArt("ext-deezer-album-42")
        assertTrue(CoverArtCache.needsRevalidation(cover))
        CoverArtCache.markRevalidated(cover)
        assertFalse(CoverArtCache.needsRevalidation(cover))
    }
}
