package com.udeshcoffee.android.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Runtime permissions the app needs, per API level. */
object Permissions {

    /** Permission that grants read access to the device's audio files. */
    val readAudio: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    /** Everything requested on first launch. Only [readAudio] is mandatory. */
    fun startupPermissions(): Array<String> = buildList {
        add(readAudio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        // Below API 29 editing tags and deleting files writes to shared storage directly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }.toTypedArray()

    fun hasReadAudio(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, readAudio) == PackageManager.PERMISSION_GRANTED
}
