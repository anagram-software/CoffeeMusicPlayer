package com.udeshcoffee.android.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File
import java.io.IOException

/**
 * Root directories of the mounted shared-storage volumes, primary volume first,
 * paired with a display name.
 */
fun getStorageRoots(context: Context): List<Pair<String, File>> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val storageManager = context.getSystemService(StorageManager::class.java)
        return storageManager.storageVolumes
                .filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
                .sortedByDescending { it.isPrimary }
                .mapNotNull { volume ->
                    volume.directory?.let { dir ->
                        (if (volume.isPrimary) "Internal" else volume.getDescription(context)) to dir
                    }
                }
    }

    // API 26-29: primary storage plus removable volumes found through the app's external files dirs.
    val roots = ArrayList<Pair<String, File>>()
    @Suppress("DEPRECATION")
    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
        roots.add("Internal" to Environment.getExternalStorageDirectory())
    val primaryAppDir = context.getExternalFilesDir(null)
    for (dir in context.getExternalFilesDirs(null)) {
        if (dir == null || dir == primaryAppDir) continue
        val index = dir.absolutePath.lastIndexOf("/Android/data")
        if (index < 0) continue
        val root = File(dir.absolutePath.substring(0, index))
        roots.add("SD Card" to (try { root.canonicalFile } catch (e: IOException) { root }))
    }
    return roots
}
