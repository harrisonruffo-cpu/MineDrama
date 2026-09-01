package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.Appwrite
import com.google.firebase.firestore.FirebaseFirestore
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class DiagnosticItem(
    val service: String,
    val target: String,
    val isSuccess: Boolean,
    val message: String,
    val hint: String? = null
)

data class CloudDiagnosticResult(
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<DiagnosticItem>,
    val summary: String
)

class CloudDiagnosticManager(private val context: Context) {
    private val TAG = "CloudDiagnostic"

    suspend fun runDiagnostic(): CloudDiagnosticResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiagnosticItem>()

        // 1. Appwrite Client & Session
        try {
            if (!Appwrite.isInitialized) {
                Appwrite.init(context)
            }
            val sessionOk = Appwrite.ensureSession()
            if (sessionOk) {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Auth",
                        target = "Sessão Anônima / Usuário",
                        isSuccess = true,
                        message = "Conectado e autenticado com sucesso no Appwrite NYC Cloud (Project: ${Appwrite.PROJECT_ID})"
                    )
                )
            } else {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Auth",
                        target = "Sessão Anônima",
                        isSuccess = false,
                        message = "Falha ao criar/recuperar sessão de usuário.",
                        hint = "Verifique se o Project ID '${Appwrite.PROJECT_ID}' está correto no Appwrite."
                    )
                )
            }
        } catch (e: Exception) {
            results.add(
                DiagnosticItem(
                    service = "Appwrite Auth",
                    target = "Inicialização",
                    isSuccess = false,
                    message = "Erro: ${e.message}",
                    hint = "Verifique a conexão de internet ou Endpoint do Appwrite."
                )
            )
        }

        // 2. Appwrite Database (Database: litoral_novelas, Collection: dramas)
        try {
            val response = Appwrite.databases.listDocuments(
                databaseId = Appwrite.DATABASE_ID,
                collectionId = Appwrite.COLLECTION_DRAMAS,
                queries = listOf(Query.limit(1))
            )
            results.add(
                DiagnosticItem(
                    service = "Appwrite Database",
                    target = "DB: ${Appwrite.DATABASE_ID} / Col: ${Appwrite.COLLECTION_DRAMAS}",
                    isSuccess = true,
                    message = "Sucesso! Total de novelas encontradas: ${response.total}"
                )
            )
        } catch (e: Exception) {
            val err = e.message ?: "Desconhecido"
            val hint = when {
                err.contains("404") || err.contains("not found", ignoreCase = true) ->
                    "Banco '${Appwrite.DATABASE_ID}' ou Coleção '${Appwrite.COLLECTION_DRAMAS}' não foi criada no Appwrite."
                err.contains("401") || err.contains("unauthorized", ignoreCase = true) || err.contains("permission", ignoreCase = true) ->
                    "Permissão negada na Coleção '${Appwrite.COLLECTION_DRAMAS}'. Vá em Databases > dramas > Settings > Permissions > adicione 'Any' (Create, Read, Update, Delete)."
                else -> "Erro retornado: $err"
            }
            results.add(
                DiagnosticItem(
                    service = "Appwrite Database",
                    target = "DB: ${Appwrite.DATABASE_ID} / Col: ${Appwrite.COLLECTION_DRAMAS}",
                    isSuccess = false,
                    message = "Erro ao ler/gravar dados: $err",
                    hint = hint
                )
            )
        }

        // 3. Appwrite Storage Bucket 'videos'
        try {
            val fileList = Appwrite.storage.listFiles(
                bucketId = Appwrite.BUCKET_VIDEOS,
                queries = listOf(Query.limit(1))
            )
            results.add(
                DiagnosticItem(
                    service = "Appwrite Storage",
                    target = "Bucket: ${Appwrite.BUCKET_VIDEOS}",
                    isSuccess = true,
                    message = "Sucesso! Acesso liberado ao bucket de vídeos (${fileList.total} arquivos)."
                )
            )
        } catch (e: Exception) {
            val err = e.message ?: "Desconhecido"
            val hint = when {
                err.contains("404") || err.contains("not found", ignoreCase = true) ->
                    "Bucket '${Appwrite.BUCKET_VIDEOS}' não existe no Appwrite. Crie em Storage > Create Bucket > ID: '${Appwrite.BUCKET_VIDEOS}'."
                err.contains("401") || err.contains("unauthorized", ignoreCase = true) || err.contains("permission", ignoreCase = true) ->
                    "Permissão negada no Bucket '${Appwrite.BUCKET_VIDEOS}'. Vá em Storage > videos > Settings > Permissions > adicione 'Any' (Create, Read, Update)."
                else -> "Erro no Storage: $err"
            }
            results.add(
                DiagnosticItem(
                    service = "Appwrite Storage",
                    target = "Bucket: ${Appwrite.BUCKET_VIDEOS}",
                    isSuccess = false,
                    message = "Falha no Bucket de Vídeos: $err",
                    hint = hint
                )
            )
        }

        // 4. Appwrite Storage Bucket 'covers'
        try {
            val fileList = Appwrite.storage.listFiles(
                bucketId = Appwrite.BUCKET_COVERS,
                queries = listOf(Query.limit(1))
            )
            results.add(
                DiagnosticItem(
                    service = "Appwrite Storage",
                    target = "Bucket: ${Appwrite.BUCKET_COVERS}",
                    isSuccess = true,
                    message = "Sucesso! Acesso liberado ao bucket de capas (${fileList.total} arquivos)."
                )
            )
        } catch (e: Exception) {
            val err = e.message ?: "Desconhecido"
            val hint = when {
                err.contains("404") || err.contains("not found", ignoreCase = true) ->
                    "Bucket '${Appwrite.BUCKET_COVERS}' não existe no Appwrite. Crie em Storage > Create Bucket > ID: '${Appwrite.BUCKET_COVERS}'."
                err.contains("401") || err.contains("unauthorized", ignoreCase = true) || err.contains("permission", ignoreCase = true) ->
                    "Permissão negada no Bucket '${Appwrite.BUCKET_COVERS}'. Vá em Storage > covers > Settings > Permissions > adicione 'Any' (Create, Read, Update)."
                else -> "Erro no Storage: $err"
            }
            results.add(
                DiagnosticItem(
                    service = "Appwrite Storage",
                    target = "Bucket: ${Appwrite.BUCKET_COVERS}",
                    isSuccess = false,
                    message = "Falha no Bucket de Capas: $err",
                    hint = hint
                )
            )
        }

        // 5. Firebase Firestore
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snap = firestore.collection("dramas").limit(1).get().await()
            results.add(
                DiagnosticItem(
                    service = "Firebase Firestore",
                    target = "Coleção: dramas",
                    isSuccess = true,
                    message = "Conexão ativa! Documentos disponíveis: ${snap.size()}"
                )
            )
        } catch (e: Exception) {
            results.add(
                DiagnosticItem(
                    service = "Firebase Firestore",
                    target = "Coleção: dramas",
                    isSuccess = false,
                    message = "Aviso: ${e.message}",
                    hint = "Verifique as Regras de Segurança no Console do Firebase."
                )
            )
        }

        val successCount = results.count { it.isSuccess }
        val totalCount = results.size
        val summary = if (successCount == totalCount) {
            "✅ Todos os serviços de Nuvem estão 100% operacionais e autorizados!"
        } else {
            "⚠️ $successCount de $totalCount serviços conectados. Verifique as dicas abaixo para liberar o acesso."
        }

        CloudDiagnosticResult(
            items = results,
            summary = summary
        )
    }
}
