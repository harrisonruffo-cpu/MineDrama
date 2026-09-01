package com.example.data.remote

import android.util.Log
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreDramaDataSource {
    private val TAG = "FirestoreDramaDS"
    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Firestore indisponível: ${e.message}")
            null
        }

    suspend fun getDramas(): List<Drama> = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext emptyList()
            val snapshot = db.collection("dramas")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: "Sem título"
                    val desc = doc.getString("description") ?: ""
                    val cover = doc.getString("coverUrl") ?: ""
                    val banner = doc.getString("bannerUrl") ?: ""
                    val genre = doc.getString("genre") ?: "Romance"
                    val rating = doc.getDouble("rating") ?: 4.8
                    val views = doc.getLong("viewsCount") ?: 1000L
                    val likes = doc.getLong("likesCount") ?: 250L
                    val isTrend = doc.getBoolean("isTrending") ?: false
                    val isFeat = doc.getBoolean("isFeatured") ?: false

                    val rawEpisodes = doc.get("episodes") as? List<Map<String, Any>> ?: emptyList()
                    val episodes = rawEpisodes.mapIndexed { idx, epMap ->
                        Episode(
                            id = epMap["id"] as? String ?: "${id}_$idx",
                            dramaId = id,
                            episodeNumber = (epMap["episodeNumber"] as? Long)?.toInt() ?: (idx + 1),
                            title = epMap["title"] as? String ?: "Episódio ${idx + 1}",
                            videoUrl = epMap["videoUrl"] as? String ?: "",
                            durationSeconds = (epMap["durationSeconds"] as? Long)?.toInt() ?: 90,
                            isFree = epMap["isFree"] as? Boolean ?: true,
                            thumbnail = epMap["thumbnail"] as? String ?: ""
                        )
                    }

                    Drama(
                        id = id,
                        title = title,
                        description = desc,
                        coverUrl = cover,
                        bannerUrl = banner,
                        genre = genre,
                        totalEpisodes = episodes.size.coerceAtLeast(1),
                        rating = rating,
                        viewsCount = views,
                        likesCount = likes,
                        isTrending = isTrend,
                        isFeatured = isFeat,
                        episodes = episodes
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar do Firestore: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveDrama(drama: Drama): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext false
            val episodesMap = drama.episodes.map { ep ->
                mapOf(
                    "id" to ep.id,
                    "dramaId" to ep.dramaId,
                    "episodeNumber" to ep.episodeNumber,
                    "title" to ep.title,
                    "videoUrl" to ep.videoUrl,
                    "durationSeconds" to ep.durationSeconds,
                    "isFree" to ep.isFree,
                    "thumbnail" to ep.thumbnail
                )
            }

            val docData = hashMapOf(
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
                "createdAt" to drama.createdAt,
                "episodes" to episodesMap
            )

            db.collection("dramas")
                .document(drama.id)
                .set(docData, SetOptions.merge())
                .await()

            true
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao salvar no Firestore: ${e.message}")
            false
        }
    }
}
