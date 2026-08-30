package com.example.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaStorageHelper {

    suspend fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        subfolder: String,
        prefix: String
    ): String = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "http" || uri.scheme == "https") {
                return@withContext uri.toString()
            }

            val dir = File(context.filesDir, subfolder).apply {
                if (!exists()) mkdirs()
            }

            val extension = when (subfolder) {
                "videos" -> ".mp4"
                "covers" -> ".jpg"
                else -> ""
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}$extension"
            val destFile = File(dir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("MediaStorageHelper", "File copied successfully to ${destFile.absolutePath}")
            return@withContext Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            Log.e("MediaStorageHelper", "Failed to copy URI to internal storage", e)
            return@withContext uri.toString()
        }
    }
}
