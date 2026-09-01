package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.Appwrite
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AppwriteStorageManager {
    private const val TAG = "AppwriteStorage"

    // Substitua pelo ID do Bucket criado no Console do Appwrite
    var BUCKET_VIDEOS = "videos"
    var BUCKET_COVERS = "covers"

    /**
     * Faz upload de um arquivo para o Storage do Appwrite e retorna o link direto de visualização/streaming.
     */
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        bucketId: String,
        mimeType: String = "video/mp4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!Appwrite.isInitialized) {
                Appwrite.init(context)
            }
            Appwrite.ensureSession()

            val contentResolver = context.contentResolver
            val tempFile = File(context.cacheDir, "appwrite_upload_${System.currentTimeMillis()}")
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Não foi possível ler o arquivo selecionado"))

            val inputFile = InputFile.fromFile(tempFile)
            val fileId = ID.unique()

            // Executa o upload para o Bucket Appwrite
            val response = Appwrite.storage.createFile(
                bucketId = bucketId,
                fileId = fileId,
                file = inputFile
            )

            // Constrói URL direta pública de visualização/stream
            val fileUrl = "https://cloud.appwrite.io/v1/storage/buckets/$bucketId/files/${response.id}/view?project=${Appwrite.PROJECT_ID}"
            Log.d(TAG, "Upload concluído com sucesso no Appwrite: $fileUrl")
            
            tempFile.delete()
            Result.success(fileUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no upload do Appwrite para bucket '$bucketId': ${e.message}", e)
            Result.failure(e)
        }
    }
}
