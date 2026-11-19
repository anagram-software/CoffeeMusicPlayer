package com.udeshcoffee.android.data.lyrics

import com.udeshcoffee.android.data.subsonic.SubLyricLine
import com.udeshcoffee.android.data.subsonic.SubStructuredLyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {

    @Test fun parsesTimestampsAndSorts() {
        val l = parseLrc("[ar:Muse]\n[00:12.50]Second\n[00:01.00]First\n[01:02.345]Third")
        assertTrue(l.synced)
        assertEquals(listOf("First", "Second", "Third"), l.lines.map { it.text })
        assertEquals(listOf(1_000L, 12_500L, 62_345L), l.lines.map { it.startMs })
    }

    @Test fun repeatedTimestampsAndOffset() {
        val l = parseLrc("[offset:+500]\n[00:01.00][00:10.00]Chorus\n[00:00.20]Start")
        assertEquals(listOf("Start", "Chorus", "Chorus"), l.lines.map { it.text })
        assertEquals(listOf(0L, 500L, 9_500L), l.lines.map { it.startMs })
    }

    @Test fun plainTextIsUnsyncedAndTrimmed() {
        val l = parseLrc("\n[ti:Song]\nHello\n\nWorld\n\n")
        assertFalse(l.synced)
        assertEquals(listOf("Hello", "", "World"), l.lines.map { it.text })
        assertTrue(l.lines.all { it.startMs == null })
    }

    @Test fun structuredPrefersSyncedMainLyrics() {
        val plain = SubStructuredLyrics(false, listOf(SubLyricLine(null, "plain")))
        val translation = SubStructuredLyrics(true, listOf(SubLyricLine(0, "trans")), kind = "translation")
        val synced = SubStructuredLyrics(true, listOf(SubLyricLine(2_000, "b"), SubLyricLine(1_000, "a")), offset = 500)
        val l = fromStructured(listOf(translation, plain, synced))!!
        assertTrue(l.synced)
        assertEquals(listOf("a", "b"), l.lines.map { it.text })
        assertEquals(listOf(500L, 1_500L), l.lines.map { it.startMs })
    }

    @Test fun structuredEmptyIsNull() {
        assertNull(fromStructured(listOf(SubStructuredLyrics(true, emptyList()))))
    }

    @Test fun activeLine() {
        val lines = listOf(LyricLine("a", 1_000), LyricLine("b", 2_000), LyricLine("c", 3_000))
        assertEquals(-1, activeLineIndex(lines, 500))
        assertEquals(0, activeLineIndex(lines, 1_000))
        assertEquals(1, activeLineIndex(lines, 2_999))
        assertEquals(2, activeLineIndex(lines, 10_000))
        assertEquals(-1, activeLineIndex(emptyList(), 10_000))
    }

    @Test fun sectionHeaders() {
        assertTrue(isSectionHeader("[Chorus]"))
        assertTrue(isSectionHeader("Verse 2:"))
        assertFalse(isSectionHeader("I want to break free"))
    }
}
