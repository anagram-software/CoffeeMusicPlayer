package com.udeshcoffee.android.utils

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.provider.DocumentFile
import android.util.Log
import com.udeshcoffee.android.App
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.*

/**
 * Created by Udathari on 11/24/2017.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
fun getExtSdCardFiles(): Array<File> {
    val files = getExtSdCardPaths().map { File(it) }
    return files.toTypedArray()
}

@TargetApi(Build.VERSION_CODES.KITKAT)
fun getExtSdCardPaths(): Array<String> {
    val paths = ArrayList<String>()
    for (file in App.Companion.instance.getExternalFilesDirs("external")) {

        if (file != null && file != App.Companion.instance.getExternalFilesDir("external")) {
            val index = file.absolutePath.lastIndexOf("/Android/data")
            if (index < 0) {
                Log.w("asd", "Unexpected external file dir: " + file.absolutePath)
            } else {
                var path = file.absolutePath.substring(0, index)
                try {
                    path = File(path).canonicalPath
                } catch (e: IOException) {
                    // Keep non-canonical path.
                }

                paths.add(path)
            }
        }
    }
    return paths.toTypedArray()
}

@Throws(IOException::class)
fun copyFile(sourceFile: File, destFile: File) {

    var destination: FileChannel? = null
    var source: FileChannel? = null
    var pfd: ParcelFileDescriptor? = null
    try {
        source = FileInputStream(sourceFile).channel

        if (isFromSdCard(destFile.absolutePath)) {
            val documentFile = getDocumentFile(destFile)
            pfd = App.instance.contentResolver.openFileDescriptor(documentFile?.uri, "w")
            val outputStream = FileOutputStream(pfd!!.fileDescriptor)
            destination = outputStream.channel
        } else {
            destination = FileOutputStream(destFile).channel
        }


        if (!destFile.parentFile.exists())
            destFile.parentFile.mkdirs()

        if (!destFile.exists()) {
            destFile.createNewFile()
        }


        destination!!.transferFrom(source, 0, source!!.size())

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (pfd != null) {
            pfd.close()
        }
        if (source != null) {
            source.close()
        }
        if (destination != null) {
            destination.close()
        }
    }
}

fun cutFile(sourceFile: File, destFile: File){
    var destination: FileChannel? = null
    var source: FileChannel? = null
    var pfd: ParcelFileDescriptor? = null
    try {
        source = FileInputStream(sourceFile).channel

        if (isFromSdCard(destFile.absolutePath)) {
            val documentFile = getDocumentFile(destFile)
            pfd = App.instance.contentResolver.openFileDescriptor(documentFile?.uri, "w")
            val outputStream = FileOutputStream(pfd!!.fileDescriptor)
            destination = outputStream.channel
        } else {
            destination = FileOutputStream(destFile).channel
        }


        if (!destFile.parentFile.exists())
            destFile.parentFile.mkdirs()

        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        destination!!.transferFrom(source, 0, source!!.size())
    } catch (e: Exception) {
        throw e
    } finally {
        deleteRecursive(sourceFile)
        if (pfd != null) {
            pfd.close()
        }
        if (source != null) {
            source.close()
        }
        if (destination != null) {
            destination.close()
        }
    }
}

@TargetApi(Build.VERSION_CODES.KITKAT)
fun getDocumentFile(file: File): DocumentFile? {
    val baseFolder = getExtSdCardFolder(file)
    val relativePath: String?

    if (baseFolder == null) {
        return null
    }

    try {
        val fullPath = file.canonicalPath
        relativePath = fullPath.substring(baseFolder.length + 1)
    } catch (e: IOException) {
        return null
    }

    val treeUri = App.instance.contentResolver.persistedUriPermissions[0].uri ?: return null

// start with root of SD card and then parse through document tree.
    var document = DocumentFile.fromTreeUri(App.instance, treeUri)

    val parts = relativePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    parts
            .asSequence()
            .mapNotNull { document.findFile(it) }
            .forEach { document = it }

    return document
}


fun getExtSdCardFolder(file: File): String? {
    val extSdPaths = getExtSdCardPaths()
    try {
        extSdPaths.indices
                .filter { file.canonicalPath.startsWith(extSdPaths[it]) }
                .forEach { return extSdPaths[it] }
    } catch (e: IOException) {
        return null
    }

    return null
}

fun deleteRecursive(fileOrDirectory: File): Boolean {
    if (fileOrDirectory.isDirectory)
        if (fileOrDirectory.listFiles() != null)
            for (child in fileOrDirectory.listFiles()) {
                return deleteRecursive(child)
            }

    if (!isFromSdCard(fileOrDirectory.absolutePath)) {
        if (fileOrDirectory.delete()) {
            return deleteViaContentProvider(fileOrDirectory.absolutePath)
        }

    } else {
        val documentFile = getDocumentFile(fileOrDirectory)
        documentFile?.let {
            if (it.delete()) {
                return deleteViaContentProvider(fileOrDirectory.absolutePath)
            }
        }
    }
    return false
}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
fun deleteViaContentProvider(fullname: String): Boolean {
    val uri = getFileUri(App.instance, fullname) ?: return false
    return try {
        val resolver = App.instance.contentResolver
        // change type to image, otherwise nothing will be deleted
        val contentValues = ContentValues()
        val mediaType = 1
        contentValues.put("media_type", mediaType)
        resolver.update(uri, contentValues, null, null)

        resolver.delete(uri, null, null) > 0
    } catch (e: Throwable) {
        false
    }

}


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
private fun getFileUri(context: Context, fullname: String): Uri? {
    var uri: Uri?
    val cursor: Cursor?
    val contentResolver: ContentResolver?
    try {
        contentResolver = context.contentResolver
        uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOfNulls<String>(2)
        projection[0] = "_id"
        projection[1] = "_data"
        val selection = "_data = ? "    // this avoids SQL injection
        val selectionParams = arrayOfNulls<String>(1)
        selectionParams[0] = fullname
        val sortOrder = "_id"
        cursor = contentResolver!!.query(uri!!, projection, selection, selectionParams, sortOrder)
        if (cursor != null) {
            try {
                if (cursor.count > 0)
                // file present!
                {
                    cursor.moveToFirst()
                    val dataColumn = cursor.getColumnIndex("_data")
                    val s = cursor.getString(dataColumn)
                    if (s != fullname)
                        return null
                    val idColumn = cursor.getColumnIndex("_id")
                    val id = cursor.getLong(idColumn)
                    uri = MediaStore.Files.getContentUri("external", id)
                } else
                // file isn't in the media database!
                {
                    val contentValues = ContentValues()
                    contentValues.put("_data", fullname)
                    uri = MediaStore.Files.getContentUri("external")
                    uri = contentResolver.insert(uri!!, contentValues)
                }
            } catch (e: Throwable) {
                uri = null
            } finally {
                cursor.close()
            }
        }
    } catch (e: Throwable) {
        uri = null
    }

    return uri
}

fun isFromSdCard(filepath: String): Boolean {
    val path1 = Environment.getExternalStorageDirectory().toString()
    return !filepath.startsWith(path1)

}

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
fun hasPermission(): Boolean {
    val uriPermission = App.instance.contentResolver.persistedUriPermissions
    return uriPermission != null && uriPermission.size > 0
}