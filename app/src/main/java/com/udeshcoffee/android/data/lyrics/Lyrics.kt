package com.udeshcoffee.android.data.lyrics

import com.udeshcoffee.android.data.subsonic.SubStructuredLyrics

/** One lyric line; [startMs] is set for synced lyrics only (milliseconds from the start of the track). */
data class LyricLine(val text: String, val startMs: Long? = null)

/** A song's lyrics. When [synced], every line has a [LyricLine.startMs], in ascending order. */
data class Lyrics(val synced: Boolean, val lines: List<LyricLine>)

private val offsetTag = Regex("""^\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)
private val timestamp = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.:]\d{1,3})?)]""")
private val anyTag = Regex("""\[[^\]]*]""")
private val metadataTag = Regex("""^\[[a-z]+:.*]$""", RegexOption.IGNORE_CASE)

/**
 * Parses LRC: "[mm:ss.xx]text", possibly several timestamps per line, plus an optional
 * "[offset:±ms]" tag (positive = show sooner). Text without timestamps comes back unsynced.
 * Same rules as the web player's `parseLrc`.
 */
fun parseLrc(lrc: String): Lyrics {
    var offset = 0L
    val timed = mutableListOf<LyricLine>()
    val plain = mutableListOf<LyricLine>()
    for (raw in lrc.lines()) {
        offsetTag.find(raw)?.let {
            offset = it.groupValues[1].toLong()
            continue
        }
        val stamps = timestamp.findAll(raw).toList()
        val text = raw.replace(anyTag, "").trim()
        if (stamps.isEmpty()) {
            // Skip metadata tags like [ar:Artist]
            if (!metadataTag.matches(raw.trim())) plain += LyricLine(raw.trim())
            continue
        }
        for (m in stamps) {
            val seconds = m.groupValues[1].toLong() * 60 + m.groupValues[2].replace(':', '.').toDouble()
            timed += LyricLine(text, Math.round(seconds * 1000))
        }
    }
    if (timed.isEmpty()) return Lyrics(false, plain.trimBlank())
    return Lyrics(true, timed.sortedBy { it.startMs }.map { it.copy(startMs = (it.startMs!! - offset).coerceAtLeast(0)) })
}

private fun List<LyricLine>.trimBlank(): List<LyricLine> =
        dropWhile { it.text.isEmpty() }.dropLastWhile { it.text.isEmpty() }

/** Picks the best OpenSubsonic structured lyrics: main vocals, synced if available. Null when there are none. */
fun fromStructured(list: List<SubStructuredLyrics>): Lyrics? {
    val all = list.filter { it.line.isNotEmpty() }
    if (all.isEmpty()) return null
    val pool = all.filter { it.kind == null || it.kind == "main" }.ifEmpty { all }
    val best = pool.firstOrNull { it.synced } ?: pool.first()
    val synced = best.synced && best.line.all { it.start != null }
    return Lyrics(synced, best.line.map { l ->
        LyricLine(l.value, if (synced) (l.start!! - best.offset).coerceAtLeast(0) else null)
    }.let { if (synced) it.sortedBy { l -> l.startMs } else it })
}

/** Index of the line being sung at [timeMs], or -1 before the first line. */
fun activeLineIndex(lines: List<LyricLine>, timeMs: Long): Int {
    var lo = 0
    var hi = lines.size - 1
    var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if ((lines[mid].startMs ?: 0) <= timeMs) {
            found = mid
            lo = mid + 1
        } else hi = mid - 1
    }
    return found
}

private val bracketHeader = Regex("""^\[.+]$""")
private val namedHeader = Regex("""^(verse|chorus|pre-chorus|bridge|intro|outro|hook)\b.*:?$""", RegexOption.IGNORE_CASE)

/** Lines like "[Chorus]" or "Verse 2:" are section headers, shown in the accent colour. */
fun isSectionHeader(text: String): Boolean = bracketHeader.matches(text) || namedHeader.matches(text)
