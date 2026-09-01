package com.example

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Appwrite {
    private const val TAG = "AppwriteManager"

    // Região selecionada pelo usuário: NYC Cloud
    const val ENDPOINT = "https://nyc.cloud.appwrite.io/v1"
    const val PROJECT_ID = "6a956ae100291007e582"

    // Database e Coleções
    const val DATABASE_ID = "litoral_novelas"
    const val COLLECTION_DRAMAS = "dramas"
    const val COLLECTION_EPISODES = "episodes"

    // Buckets de Storage
    const val BUCKET_VIDEOS = "videos"
    const val BUCKET_COVERS = "covers"

    private lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var storage: Storage

    var isInitialized = false
        private set

    fun init(context: Context) {
        if (isInitialized) return
        try {
            client = Client(context.applicationContext)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)

            account = Account(client)
            databases = Databases(client)
            storage = Storage(client)
            isInitialized = true
            Log.d(TAG, "Appwrite inicializado com sucesso no endpoint $ENDPOINT (Project: $PROJECT_ID)")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar Appwrite: ${e.message}", e)
        }
    }

    suspend fun ensureSession(): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            try {
                val current = account.get()
                Log.d(TAG, "Sessão ativa existente: ${current.id}")
                return@withContext true
            } catch (_: Exception) {
                // Tenta criar sessão anônima
            }
            val session = account.createAnonymousSession()
            Log.d(TAG, "Nova sessão anônima criada: ${session.id}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível criar sessão anônima: ${e.message}")
            // Mesmo se a criação de sessão anônima falhar por já ter sessão ou modo guest, continua
            true
        }
    }

    fun getFileViewUrl(bucketId: String, fileId: String): String {
        return "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/view?project=$PROJECT_ID"
    }

    fun getFileDownloadUrl(bucketId: String, fileId: String): String {
        return "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/download?project=$PROJECT_ID"
    }
}
