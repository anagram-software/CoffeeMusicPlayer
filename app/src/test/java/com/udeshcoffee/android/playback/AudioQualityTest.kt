package com.udeshcoffee.android.playback

import androidx.media3.common.C
import androidx.media3.common.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioQualityTest {

    @Test
    fun `lossless shows bit depth and sample rate, not bit rate`() {
        val q = AudioQuality("audio/flac", bitrate = 4_608_000, sampleRate = 96_000, channels = 2,
                bitDepth = AudioQuality.bitDepth(C.ENCODING_PCM_24BIT))
        assertEquals("FLAC · 24-bit · 96 kHz", q.label())
    }

    @Test
    fun `lossy shows rounded bit rate`() {
        assertEquals("MP3 · 320 kbps · 44.1 kHz", AudioQuality("audio/mpeg", bitrate = 320_000, sampleRate = 44_100).label())
        assertEquals("AAC · 256 kbps · 48 kHz", AudioQuality("audio/mp4a-latm", bitrate = 255_800, sampleRate = 48_000).label())
    }

    @Test
    fun `unknown fields are left out`() {
        assertEquals("Opus · 48 kHz", AudioQuality("audio/opus", sampleRate = 48_000).label())
        assertEquals("FLAC", AudioQuality("audio/flac").label())
        assertNull(AudioQuality(null).label())
    }

    @Test
    fun `channel layouts other than stereo are named`() {
        assertEquals("MP3 · 128 kbps · Mono", AudioQuality("audio/mpeg", bitrate = 128_000, channels = 1).label())
        assertEquals("E-AC-3 · 5.1", AudioQuality("audio/eac3", channels = 6).label())
    }

    @Test
    fun `unknown mime types use their subtype`() {
        assertEquals("X-APE", AudioQuality.codecName("audio/x-ape"))
    }

    @Test
    fun `pcm encodings map to bit depths`() {
        assertEquals(16, AudioQuality.bitDepth(C.ENCODING_PCM_16BIT))
        assertEquals(32, AudioQuality.bitDepth(C.ENCODING_PCM_FLOAT))
        assertNull(AudioQuality.bitDepth(Format.NO_VALUE))
    }
}
