package com.example.data.remote

import android.util.Log
import com.example.Appwrite
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.data.model.Episode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.appwrite.ID
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remote Data Source backed by Appwrite Cloud Databases for real-time drama and episode metadata sync.
 */
class AppwriteDramaDataSource {
    private val TAG = "AppwriteDataSource"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val episodesListType = Types.newParameterizedType(List::class.java, Episode::class.java)
    private val episodesAdapter = moshi.adapter<List<Episode>>(episodesListType)
    private val tagsListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val tagsAdapter = moshi.adapter<List<String>>(tagsListType)
    private val castListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val castAdapter = moshi.adapter<List<String>>(castListType)

    suspend fun fetchDramas(): List<Drama> = withContext(Dispatchers.IO) {
        if (!Appwrite.isInitialized) return@withContext emptyList()
        Appwrite.ensureSession()

        try {
            val response = Appwrite.databases.listDocuments(
                databaseId = Appwrite.DATABASE_ID,
                collectionId = Appwrite.COLLECTION_DRAMAS,
                queries = listOf(
                    Query.limit(100)
                )
            )

            val dramas = response.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    val id = doc.id
                    val title = data["title"] as? String ?: ""
                    val originalTitle = data["originalTitle"] as? String ?: title
                    val synopsis = data["synopsis"] as? String ?: ""
                    val categoryStr = data["category"] as? String ?: "ROMANCE_CEO"
                    val category = try { DramaCategory.valueOf(categoryStr) } catch (_: Exception) { DramaCategory.ROMANCE_CEO }
                    val posterUrl = data["posterUrl"] as? String ?: ""
                    val bannerUrl = data["bannerUrl"] as? String ?: posterUrl
                    val rating = (data["rating"] as? Number)?.toDouble() ?: 4.8
                    val views = (data["views"] as? Number)?.toLong() ?: 0L
                    val likes = (data["likes"] as? Number)?.toLong() ?: 0L
                    val releaseYear = (data["releaseYear"] as? Number)?.toInt() ?: 2026
                    val director = data["director"] as? String ?: "Litoral Novelas"
                    val totalEpisodes = (data["totalEpisodes"] as? Number)?.toInt() ?: 1
                    val isTrending = (data["isTrending"] as? Boolean) ?: true
                    val isTop10 = (data["isTop10"] as? Boolean) ?: false
                    val authorId = data["authorId"] as? String ?: ""
                    val authorName = data["authorName"] as? String ?: ""
                    val authorPhotoUrl = data["authorPhotoUrl"] as? String ?: ""

                    val episodesJson = data["episodesJson"] as? String ?: "[]"
                    val episodes = try {
                        episodesAdapter.fromJson(episodesJson) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val tagsJson = data["tagsJson"] as? String ?: "[]"
                    val tags = try {
                        tagsAdapter.fromJson(tagsJson) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val castJson = data["castJson"] as? String ?: "[]"
                    val cast = try {
                        castAdapter.fromJson(castJson) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    Drama(
                        id = id,
                        title = title,
                        originalTitle = originalTitle,
                        synopsis = synopsis,
                        category = category,
                        posterUrl = posterUrl,
                        bannerUrl = bannerUrl,
                        rating = rating,
                        views = views,
                        likes = likes,
                        releaseYear = releaseYear,
                        director = director,
                        cast = cast,
                        totalEpisodes = if (totalEpisodes > 0) totalEpisodes else episodes.size,
                        isTrending = isTrending,
                        isTop10 = isTop10,
                        tags = tags,
                        episodes = episodes,
                        authorId = authorId,
                        authorName = authorName,
                        authorPhotoUrl = authorPhotoUrl
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing Appwrite drama doc ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Fetched ${dramas.size} dramas from Appwrite Database")
            dramas
        } catch (e: Exception) {
            Log.w(TAG, "Appwrite fetchDramas note: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveOrUpdateDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        if (!Appwrite.isInitialized) return@withContext false
        Appwrite.ensureSession()

        try {
            val episodesJson = episodesAdapter.toJson(drama.episodes)
            val tagsJson = tagsAdapter.toJson(drama.tags)
            val castJson = castAdapter.toJson(drama.cast)

            // Safe document map with string and primitive keys
            val documentData = mapOf(
                "title" to drama.title,
                "originalTitle" to drama.originalTitle,
                "synopsis" to drama.synopsis,
                "category" to drama.category.name,
                "posterUrl" to drama.posterUrl,
                "bannerUrl" to drama.bannerUrl,
                "rating" to drama.rating,
                "views" to drama.views,
                "likes" to drama.likes,
                "releaseYear" to drama.releaseYear,
                "director" to drama.director,
                "totalEpisodes" to drama.totalEpisodes,
                "isTrending" to drama.isTrending,
                "isTop10" to drama.isTop10,
                "authorId" to drama.authorId,
                "authorName" to drama.authorName,
                "authorPhotoUrl" to drama.authorPhotoUrl,
                "episodesJson" to episodesJson,
                "tagsJson" to tagsJson,
                "castJson" to castJson
            )

            // Sanitize drama id for Appwrite document id (only a-z, A-Z, 0-9, period, hyphen, underscore, max 36 chars)
            val safeDocId = drama.id.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(36)

            try {
                // Try creating document
                Appwrite.databases.createDocument(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    documentId = safeDocId,
                    data = documentData
                )
                Log.d(TAG, "Document $safeDocId created in Appwrite Database")
            } catch (e: Exception) {
                // If it already exists (409 Conflict) or failed, try update
                Appwrite.databases.updateDocument(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    documentId = safeDocId,
                    data = documentData
                )
                Log.d(TAG, "Document $safeDocId updated in Appwrite Database")
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Appwrite saveOrUpdateDrama note: ${e.message}")
            false
        }
    }

    suspend fun deleteDrama(dramaId: String): Boolean = withContext(Dispatchers.IO) {
        if (!Appwrite.isInitialized) return@withContext false
        Appwrite.ensureSession()
        try {
            val safeDocId = dramaId.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(36)
            Appwrite.databases.deleteDocument(
                databaseId = Appwrite.DATABASE_ID,
                collectionId = Appwrite.COLLECTION_DRAMAS,
                documentId = safeDocId
            )
            Log.d(TAG, "Document $safeDocId deleted from Appwrite")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Appwrite deleteDrama note: ${e.message}")
            false
        }
    }
}
