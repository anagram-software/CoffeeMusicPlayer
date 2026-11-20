package com.udeshcoffee.android.data.lastfm

import org.junit.Assert.assertEquals
import org.junit.Test

class LastFmAuthTest {

    @Test
    fun `signature is md5 of sorted name-value pairs plus secret`() {
        val params = mapOf("token" to "TOK", "method" to "auth.getSession", "api_key" to "KEY")
        assertEquals("0bf279e021f3a81b4553dd7e76cf72ad", LastFmAuth.signature(params, "SECRET"))
    }

    @Test
    fun `format and callback are not signed`() {
        val params = mapOf("token" to "TOK", "method" to "auth.getSession", "api_key" to "KEY")
        assertEquals(LastFmAuth.signature(params, "SECRET"),
                LastFmAuth.signature(params + mapOf("format" to "json", "callback" to "x"), "SECRET"))
    }

    @Test
    fun `auth url carries the api key and callback`() {
        val url = LastFmAuth.authUrl("KEY")
        assertEquals("www.last.fm", url.host)
        assertEquals("/api/auth/", url.encodedPath)
        assertEquals("KEY", url.queryParameter("api_key"))
        assertEquals("coffeeplayer://lastfm-auth", url.queryParameter("cb"))
    }
}
