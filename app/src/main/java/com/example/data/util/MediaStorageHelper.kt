package com.example.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object MediaStorageHelper {
    private const val TAG = "MediaStorageHelper"

    fun copyUriToLocalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val mediaDir = File(context.filesDir, "media_uploads")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            val destination = File(mediaDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Erro salvando arquivo local: ${e.message}")
            null
        }
    }
}
