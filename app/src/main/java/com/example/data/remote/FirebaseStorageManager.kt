package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * State representing an active or completed upload task to Firebase Cloud Storage.
 */
sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(
        val progressPercent: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val stepDescription: String
    ) : UploadState()
    data class Success(val downloadUrl: String) : UploadState()
    data class Error(val message: String, val canRetry: Boolean = true) : UploadState()
}

/**
 * Manager class responsible for uploading media files (MP4 videos, cover images)
 * to Firebase Cloud Storage and retrieving their permanent HTTPS download URLs.
 */
class FirebaseStorageManager(private val context: Context) {

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseStorageManager", "Firebase Storage not initialized: ${e.message}")
            null
        }
    }

    /**
     * Uploads an image (cover / banner) to Firebase Cloud Storage.
     * If the uri is already a remote HTTP/HTTPS link, it returns it directly.
     */
    suspend fun uploadCoverImage(
        uri: Uri,
        dramaId: String,
        onProgress: (progressPercent: Int, transferred: Long, total: Long) -> Unit = { _, _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        val uriStr = uri.toString()
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            return@withContext Result.success(uriStr)
        }

        val fbStorage = storage
        if (fbStorage == null) {
            return@withContext Result.failure(
                IllegalStateException("Firebase Storage não está inicializado. Verifique a conexão com a nuvem.")
            )
        }

        try {
            val fileName = "cover_${dramaId}_${System.currentTimeMillis()}.jpg"
            val storageRef = fbStorage.reference.child("covers/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("dramaId", dramaId)
                .build()

            val uploadTask = storageRef.putFile(uri, metadata)

            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                val transferred = snapshot.bytesTransferred
                val percent = if (total > 0) ((transferred * 100) / total).toInt() else 0
                onProgress(percent, transferred, total)
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("FirebaseStorageManager", "Cover uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Failed to upload cover image", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads an MP4 video to Firebase Cloud Storage with progress updates.
     * If the uri is already a remote HTTP/HTTPS link, it returns it directly.
     */
    suspend fun uploadVideo(
        uri: Uri,
        dramaId: String,
        episodeNumber: Int,
        onProgress: (progressPercent: Int, transferred: Long, total: Long) -> Unit = { _, _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        val uriStr = uri.toString()
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            return@withContext Result.success(uriStr)
        }

        val fbStorage = storage
        if (fbStorage == null) {
            return@withContext Result.failure(
                IllegalStateException("Firebase Storage não está configurado.")
            )
        }

        try {
            val fileName = "ep_${episodeNumber}_${System.currentTimeMillis()}.mp4"
            val storageRef = fbStorage.reference.child("videos/$dramaId/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("video/mp4")
                .setCustomMetadata("dramaId", dramaId)
                .setCustomMetadata("episodeNumber", episodeNumber.toString())
                .build()

            val uploadTask = storageRef.putFile(uri, metadata)

            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                val transferred = snapshot.bytesTransferred
                val percent = if (total > 0) ((transferred * 100) / total).toInt() else 0
                onProgress(percent, transferred, total)
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("FirebaseStorageManager", "Video uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("FirebaseStorageManager", "Failed to upload video", e)
            Result.failure(e)
        }
    }
}
