package com.udeshcoffee.android.data.subsonic

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicClientTest {

    @Test
    fun `token matches the Subsonic API documentation example`() {
        assertEquals("26719a1196d2a940705a59634eb18eab", SubsonicAuth.token("sesame", "c19b2d"))
    }

    @Test
    fun `salts are random hex`() {
        val a = SubsonicAuth.newSalt()
        assertTrue(a.matches(Regex("[0-9a-f]{16}")))
        assertFalse(a == SubsonicAuth.newSalt())
    }

    @Test
    fun `normalises server addresses`() {
        assertEquals("https://music.example.com", SubsonicAuth.normaliseUrl(" music.example.com/ "))
        assertEquals("http://192.168.1.5:4533", SubsonicAuth.normaliseUrl("http://192.168.1.5:4533/rest"))
        assertEquals("https://example.com/navidrome", SubsonicAuth.normaliseUrl("https://example.com/navidrome/rest/"))
    }

    @Test
    fun `urls carry token auth but never the password`() {
        val credentials = SubsonicAuth.credentials("music.example.com", "alice", "secret password")
        val url = SubsonicClient(credentials) { OkHttpClient() }.url("stream", listOf("id" to "abc", "maxBitRate" to 128, "skip" to null))
        assertEquals("https://music.example.com/rest/stream", url.toString().substringBefore('?'))
        assertEquals("alice", url.queryParameter("u"))
        assertEquals(SubsonicAuth.token("secret password", credentials.salt), url.queryParameter("t"))
        assertEquals("json", url.queryParameter("f"))
        assertEquals("128", url.queryParameter("maxBitRate"))
        assertEquals(null, url.queryParameter("skip"))
        assertFalse(url.toString().contains("secret"))
        assertEquals(null, url.queryParameter("p"))
    }

    @Test
    fun `parse returns the response body`() {
        val body = SubsonicClient.parse("""{"subsonic-response":{"status":"ok","version":"1.16.1","openSubsonic":true,"type":"navidrome"}}""")
        assertEquals("\"navidrome\"", body["type"].toString())
    }

    @Test
    fun `parse turns failed responses into exceptions`() {
        val e = assertThrows(SubsonicException::class.java) {
            SubsonicClient.parse("""{"subsonic-response":{"status":"failed","error":{"code":40,"message":"Wrong username or password"}}}""")
        }
        assertEquals(40, e.code)
        assertTrue(e.isAuthError)
        assertEquals("Wrong username or password", e.message)
    }

    @Test
    fun `parse rejects things that are not Subsonic servers`() {
        assertThrows(SubsonicException::class.java) { SubsonicClient.parse("<html>Not found</html>") }
        assertThrows(SubsonicException::class.java) { SubsonicClient.parse("""{"hello":"world"}""") }
    }
}
