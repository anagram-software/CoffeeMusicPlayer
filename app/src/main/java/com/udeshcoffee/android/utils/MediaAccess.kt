package com.udeshcoffee.android.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Scoped-storage helpers for modifying audio files the app doesn't own.
 *
 * From API 30 the user must consent through a system dialog (an [IntentSender]) before the app
 * may write to or delete another app's media. Below API 30 the legacy storage permissions apply
 * (API 29 uses `requestLegacyExternalStorage`), so no consent dialog is needed.
 */
object MediaAccess {

    fun songUri(songId: Long): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

    /** Consent dialog for editing [uris], or `null` when none is needed on this API level. */
    fun writeRequest(resolver: ContentResolver, uris: List<Uri>): IntentSender? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                MediaStore.createWriteRequest(resolver, uris).intentSender
            else null

    /** Consent dialog for deleting [uris], or `null` when none is needed on this API level. */
    fun deleteRequest(resolver: ContentResolver, uris: List<Uri>): IntentSender? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                MediaStore.createDeleteRequest(resolver, uris).intentSender
            else null
}
