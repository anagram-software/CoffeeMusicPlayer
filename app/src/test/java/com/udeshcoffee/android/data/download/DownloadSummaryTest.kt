package com.udeshcoffee.android.data.download

import com.udeshcoffee.android.data.subsonic.ServerSong
import com.udeshcoffee.android.data.subsonic.toSong
import com.udeshcoffee.android.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSummaryTest {

    private fun server(id: String): Song = ServerSong(id = id, title = id, album = "Album", artist = "Artist").toSong()

    private fun entry(song: Song, status: DownloadStatus, progress: Float = 0f) =
            song.remoteId!! to SongDownload(song, status, progress, 0)

    @Test
    fun `nothing downloaded`() {
        val summary = downloadSummary(listOf(server("a"), server("b")), emptyMap())
        assertEquals(DownloadSummary(total = 2, done = 0, active = 0, progress = 0f), summary)
        assertFalse(summary.allDone)
        assertFalse(summary.isDownloading)
    }

    @Test
    fun `counts finished and running downloads, with averaged progress`() {
        val a = server("a")
        val b = server("b")
        val c = server("c")
        val d = server("d")
        val summary = downloadSummary(listOf(a, b, c, d), mapOf(
                entry(a, DownloadStatus.DONE),
                entry(b, DownloadStatus.DOWNLOADING, 0.5f),
                entry(c, DownloadStatus.QUEUED),
                entry(d, DownloadStatus.FAILED)))
        assertEquals(4, summary.total)
        assertEquals(1, summary.done)
        assertEquals(2, summary.active)
        assertEquals(1.5f / 4, summary.progress, 0.0001f)
        assertTrue(summary.isDownloading)
        assertFalse(summary.allDone)
    }

    @Test
    fun `all downloaded, ignoring device songs and duplicates`() {
        val a = server("a")
        val device = com.udeshcoffee.android.song(7)
        val summary = downloadSummary(listOf(a, a, device), mapOf(entry(a, DownloadStatus.DONE)))
        assertEquals(1, summary.total)
        assertTrue(summary.allDone)
    }

    @Test
    fun `device songs only`() {
        val summary = downloadSummary(listOf(com.udeshcoffee.android.song(1)), emptyMap())
        assertEquals(0, summary.total)
        assertFalse(summary.allDone)
    }
}
