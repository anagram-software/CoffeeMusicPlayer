package com.udeshcoffee.android.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The audio format being played, as the player decodes it: for server songs that's the stream
 * after any transcoding, for downloads the stored copy. Fields are null when unknown.
 */
data class AudioQuality(
        val mimeType: String?,
        val bitrate: Int? = null,
        val sampleRate: Int? = null,
        val channels: Int? = null,
        /** Bits per sample, for lossless and PCM formats (from the PCM encoding). */
        val bitDepth: Int? = null
) {
    val codec: String? get() = mimeType?.let { codecName(it) }
    val isLossless: Boolean get() = mimeType in LOSSLESS

    /** E.g. "FLAC · 24-bit · 96 kHz" or "MP3 · 320 kbps · 44.1 kHz"; null when nothing is known. */
    fun label(): String? {
        val parts = buildList {
            codec?.let(::add)
            if (isLossless) bitDepth?.let { add("$it-bit") }
            else bitrate?.let { add("${(it / 1000.0).roundToInt()} kbps") }
            sampleRate?.let { add(formatSampleRate(it)) }
            when (channels) {
                null, 2 -> {}
                1 -> add("Mono")
                6 -> add("5.1")
                8 -> add("7.1")
                else -> add("$channels ch")
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    companion object {
        private val LOSSLESS = setOf("audio/flac", "audio/alac", "audio/raw", "audio/wav", "audio/true-hd")

        fun codecName(mimeType: String): String = when (mimeType) {
            "audio/flac" -> "FLAC"
            "audio/alac" -> "ALAC"
            "audio/raw", "audio/wav" -> "PCM"
            "audio/mpeg", "audio/mpeg-L1", "audio/mpeg-L2" -> "MP3"
            "audio/mp4a-latm" -> "AAC"
            "audio/opus" -> "Opus"
            "audio/vorbis" -> "Vorbis"
            "audio/ac3" -> "AC-3"
            "audio/eac3", "audio/eac3-joc" -> "E-AC-3"
            "audio/ac4" -> "AC-4"
            "audio/true-hd" -> "TrueHD"
            "audio/vnd.dts", "audio/vnd.dts.hd" -> "DTS"
            "audio/amr-wb" -> "AMR-WB"
            "audio/3gpp" -> "AMR"
            else -> mimeType.substringAfter('/').uppercase(Locale.ROOT)
        }

        /** 44100 → "44.1 kHz", 48000 → "48 kHz". */
        fun formatSampleRate(hz: Int): String {
            val khz = hz / 1000.0
            return if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(Locale.ROOT, "%.1f kHz", khz)
        }

        @OptIn(UnstableApi::class)
        fun bitDepth(pcmEncoding: Int): Int? = when (pcmEncoding) {
            C.ENCODING_PCM_8BIT -> 8
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 16
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN, C.ENCODING_PCM_FLOAT -> 32
            else -> null
        }

        @OptIn(UnstableApi::class)
        fun from(format: Format): AudioQuality = AudioQuality(
                mimeType = format.sampleMimeType,
                bitrate = (format.averageBitrate.takeIf { it > 0 } ?: format.peakBitrate.takeIf { it > 0 }),
                sampleRate = format.sampleRate.takeIf { it > 0 },
                channels = format.channelCount.takeIf { it > 0 },
                bitDepth = bitDepth(format.pcmEncoding))

        /** The selected audio track's format, or null before the player has read the stream. */
        fun from(tracks: Tracks): AudioQuality? = tracks.groups
                .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
                ?.let { group -> (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { group.getTrackFormat(it) } }
                ?.let(::from)
    }
}
