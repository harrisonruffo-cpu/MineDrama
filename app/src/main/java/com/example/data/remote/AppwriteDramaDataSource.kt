package com.example.data.remote

import android.util.Log
import com.example.Appwrite
import com.example.data.model.Drama
import com.example.data.model.Episode
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppwriteDramaDataSource {
    private val TAG = "AppwriteDramaDS"

    suspend fun listDramas(): List<Drama> = withContext(Dispatchers.IO) {
        try {
            val db = Appwrite.databases ?: return@withContext emptyList()
            Appwrite.ensureSession()

            val response = db.listDocuments(
                databaseId = Appwrite.DATABASE_ID,
                collectionId = Appwrite.COLLECTION_DRAMAS,
                queries = listOf(
                    Query.limit(100),
                    Query.orderDesc("\$createdAt")
                )
            )

            response.documents.mapNotNull { doc ->
                mapDocumentToDrama(doc)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao listar dramas do Appwrite: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = Appwrite.databases ?: return@withContext false
            Appwrite.ensureSession()

            val episodesJson = JSONArray().apply {
                drama.episodes.forEach { ep ->
                    put(
                        JSONObject().apply {
                            put("id", ep.id)
                            put("dramaId", drama.id)
                            put("episodeNumber", ep.episodeNumber)
                            put("title", ep.title)
                            put("videoUrl", ep.videoUrl)
                            put("durationSeconds", ep.durationSeconds)
                            put("isFree", ep.isFree)
                            put("thumbnail", ep.thumbnail)
                        }
                    )
                }
            }.toString()

            val payload = mapOf(
                "title" to drama.title,
                "description" to drama.description,
                "coverUrl" to drama.coverUrl,
                "bannerUrl" to drama.bannerUrl,
                "genre" to drama.genre,
                "totalEpisodes" to drama.totalEpisodes,
                "rating" to drama.rating,
                "viewsCount" to drama.viewsCount,
                "likesCount" to drama.likesCount,
                "isTrending" to drama.isTrending,
                "isFeatured" to drama.isFeatured,
                "episodesJson" to episodesJson
            )

            try {
                // Tenta atualizar caso já exista
                db.updateDocument(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    documentId = drama.id,
                    data = payload
                )
                Log.d(TAG, "Drama atualizado com sucesso no Appwrite: ${drama.id}")
            } catch (_: Throwable) {
                // Se não existir, cria com o ID especificado
                db.createDocument(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    documentId = if (drama.id.isNotBlank() && drama.id.length <= 36 && !drama.id.contains(" ")) drama.id else ID.unique(),
                    data = payload
                )
                Log.d(TAG, "Drama criado com sucesso no Appwrite")
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao salvar drama no Appwrite: ${e.message}", e)
            false
        }
    }

    private fun mapDocumentToDrama(doc: Document<Map<String, Any>>): Drama? {
        return try {
            val data = doc.data
            val episodes = mutableListOf<Episode>()

            val epRaw = data["episodesJson"] as? String
            if (!epRaw.isNullOrBlank()) {
                try {
                    val arr = JSONArray(epRaw)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        episodes.add(
                            Episode(
                                id = obj.optString("id", "${doc.id}_$i"),
                                dramaId = doc.id,
                                episodeNumber = obj.optInt("episodeNumber", i + 1),
                                title = obj.optString("title", "Episódio ${i + 1}"),
                                videoUrl = obj.optString("videoUrl"),
                                durationSeconds = obj.optInt("durationSeconds", 90),
                                isFree = obj.optBoolean("isFree", true),
                                thumbnail = obj.optString("thumbnail")
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erro parse episodesJson: ${e.message}")
                }
            }

            Drama(
                id = doc.id,
                title = (data["title"] as? String) ?: "Sem Título",
                description = (data["description"] as? String) ?: "",
                coverUrl = (data["coverUrl"] as? String) ?: "",
                bannerUrl = (data["bannerUrl"] as? String) ?: "",
                genre = (data["genre"] as? String) ?: "Romance",
                totalEpisodes = (data["totalEpisodes"] as? Number)?.toInt() ?: episodes.size.coerceAtLeast(1),
                rating = (data["rating"] as? Number)?.toDouble() ?: 4.8,
                viewsCount = (data["viewsCount"] as? Number)?.toLong() ?: 1000L,
                likesCount = (data["likesCount"] as? Number)?.toLong() ?: 200L,
                isTrending = (data["isTrending"] as? Boolean) ?: false,
                isFeatured = (data["isFeatured"] as? Boolean) ?: false,
                episodes = episodes,
                isPublishedLocally = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro mapeando documento: ${e.message}")
            null
        }
    }
}
