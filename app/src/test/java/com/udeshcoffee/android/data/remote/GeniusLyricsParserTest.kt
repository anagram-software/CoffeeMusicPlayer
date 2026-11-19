package com.udeshcoffee.android.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeniusLyricsParserTest {

    private fun page(body: String) = "<html><body>$body</body></html>"

    @Test
    fun `extracts lines and highlights section headers`() {
        val html = page("""
            <div data-lyrics-container="true">
              <div data-exclude-from-selection="true">Translations Deutsch Español Song Lyrics</div>
              [Verse 1]<br>First line<br>Second &amp; line<br><br>[Chorus]<br><i>Third</i> line
            </div>
        """)
        assertEquals(
                "<font color=#1976D2><b>Verse 1</b></font><br/>First line<br/>Second & line<br/><br/>" +
                        "<font color=#1976D2><b>Chorus</b></font><br/>Third line",
                RemoteDataSource.extractLyrics(html))
    }

    @Test
    fun `joins multiple lyrics containers`() {
        val html = page("""<div data-lyrics-container="true">A<br>B</div><div>ad</div><div data-lyrics-container="true">C</div>""")
        assertEquals("A<br/>B<br/>C", RemoteDataSource.extractLyrics(html))
    }

    @Test
    fun `falls back to legacy lyrics class`() {
        assertEquals("Old<br/>Markup", RemoteDataSource.extractLyrics(page("""<div class="lyrics">Old<br>Markup</div>""")))
    }

    @Test
    fun `returns null without lyrics`() {
        assertNull(RemoteDataSource.extractLyrics(page("<p>No lyrics here</p>")))
    }
}
