package com.udeshcoffee.android.utils

import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.jaudiotagger.audio.AudioFileIO
import androidx.core.graphics.scale
import org.jaudiotagger.tag.FieldKey
import androidx.core.graphics.scale
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File

/** Tag values to write; null means "leave unchanged". */
data class TagChanges(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val genre: String? = null,
        val year: String? = null,
        val track: String? = null,
        val disc: String? = null
)

/** Writes tags and artwork into an audio file with jaudiotagger. */
object TagWriter {

    fun write(file: File, changes: TagChanges, artwork: Bitmap?) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        changes.title?.let { tag.setField(FieldKey.TITLE, it) }
        changes.artist?.let { tag.setField(FieldKey.ARTIST, it) }
        changes.album?.let { tag.setField(FieldKey.ALBUM, it) }
        changes.genre?.let { tag.setField(FieldKey.GENRE, it) }
        changes.year?.let { tag.setField(FieldKey.YEAR, it) }
        changes.track?.let { tag.setField(FieldKey.TRACK, it) }
        changes.disc?.let { tag.setField(FieldKey.DISC_NO, it) }
        artwork?.let { bitmap ->
            val scaled = bitmap.scale(500, 500)
            val bytes = ByteArrayOutputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it); it.toByteArray() }
            val artworkFile = File.createTempFile("artwork", ".jpg", file.parentFile)
            try {
                artworkFile.writeBytes(bytes)
                val art = ArtworkFactory.createArtworkFromFile(artworkFile)
                tag.deleteArtworkField()
                tag.setField(art)
            } finally {
                artworkFile.delete()
            }
        }
        audioFile.commit()
    }
}
