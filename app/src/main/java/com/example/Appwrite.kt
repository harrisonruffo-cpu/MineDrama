
package com.example

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage

object Appwrite {
    const val ENDPOINT = "https://cloud.appwrite.io/v1"
    const val PROJECT_ID = "6a956ae100291007e582"

    // Default IDs for Appwrite Cloud Database & Storage
    var DATABASE_ID = "litoral_novelas"
    var COLLECTION_DRAMAS = "dramas"
    var BUCKET_VIDEOS = "videos"
    var BUCKET_COVERS = "covers"

    lateinit var client: Client
    lateinit var account: Account
    lateinit var storage: Storage
    lateinit var databases: Databases

    var isInitialized = false
        private set

    fun init(context: Context) {
        try {
            client = Client(context)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)

            account = Account(client)
            storage = Storage(client)
            databases = Databases(client)
            isInitialized = true
        } catch (e: Exception) {
            Log.e("Appwrite", "Failed to initialize Appwrite: ${e.message}", e)
        }
    }

    suspend fun ensureSession(): Boolean {
        if (!isInitialized) return false
        return try {
            account.get()
            true
        } catch (e: Exception) {
            try {
                account.createAnonymousSession()
                Log.d("Appwrite", "Anonymous session created successfully")
                true
            } catch (e2: Exception) {
                Log.w("Appwrite", "Could not create anonymous session: ${e2.message}")
                false
            }
        }
    }
}

