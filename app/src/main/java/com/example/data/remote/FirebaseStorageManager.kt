package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseStorageManager {
    private val TAG = "FirebaseStorageManager"
    private val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Storage indisponível: ${e.message}")
            null
        }

    suspend fun uploadVideo(uri: Uri, dramaId: String, episodeNumber: Int): String? = withContext(Dispatchers.IO) {
        try {
            val fbStorage = storage ?: return@withContext null
            val fileName = "dramas/$dramaId/episodes/ep_${episodeNumber}_${UUID.randomUUID()}.mp4"
            val ref = fbStorage.reference.child(fileName)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Upload Firebase Storage concluído: $downloadUrl")
            downloadUrl
        } catch (e: Throwable) {
            Log.e(TAG, "Falha upload Firebase Storage: ${e.message}")
            null
        }
    }

    suspend fun uploadCover(uri: Uri, dramaId: String): String? = withContext(Dispatchers.IO) {
        try {
            val fbStorage = storage ?: return@withContext null
            val fileName = "dramas/$dramaId/cover_${UUID.randomUUID()}.jpg"
            val ref = fbStorage.reference.child(fileName)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Throwable) {
            Log.e(TAG, "Falha upload capa Firebase Storage: ${e.message}")
            null
        }
    }
}
