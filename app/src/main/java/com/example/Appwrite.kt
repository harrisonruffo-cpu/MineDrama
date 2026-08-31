package com.example  // 6a956ae100291007e582
import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Account
import io.appwrite.services.Storage

object Appwrite {
    // ⚠️ SUBSTITUA PELOS SEUS DADOS ⚠️
    private const val ENDPOINT = "https://cloud.appwrite.io/v1"
    private const val PROJECT_ID = "SEU_PROJECT_ID_AQUI"

    lateinit var client: Client
    lateinit var account: Account
    lateinit var storage: Storage

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)

        account = Account(client)
        storage = Storage(client)
    }
}
