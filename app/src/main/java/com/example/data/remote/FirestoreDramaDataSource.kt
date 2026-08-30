package com.example.data.remote

import android.util.Log
import com.example.data.model.Drama
import com.example.data.model.DramaCategory
import com.example.data.model.Episode
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Data Transfer Objects for Firestore serialization and real-time syncing.
 */
data class DramaFirestoreDto(
    val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val synopsis: String = "",
    val category: String = "ROMANCE_CEO",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val rating: Double = 4.8,
    val views: Long = 0L,
    val likes: Long = 0L,
    val releaseYear: Int = 2024,
    val director: String = "Direção Mine Drama",
    val cast: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
    val isTrending: Boolean = false,
    val isTop10: Boolean = false,
    val topRank: Int? = null,
    val tags: List<String> = emptyList(),
    val episodes: List<EpisodeFirestoreDto> = emptyList()
)

data class EpisodeFirestoreDto(
    val id: String = "",
    val dramaId: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val durationSeconds: Int = 60,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val synopsis: String = "",
    val isFree: Boolean = true,
    val likesCount: Long = 0L
)

fun DramaFirestoreDto.toDomain(): Drama {
    val dramaCategory = try {
        DramaCategory.valueOf(category)
    } catch (_: Exception) {
        DramaCategory.ROMANCE_CEO
    }
    return Drama(
        id = id.ifBlank { "drama_${System.currentTimeMillis()}" },
        title = title,
        originalTitle = originalTitle,
        synopsis = synopsis,
        category = dramaCategory,
        posterUrl = posterUrl,
        bannerUrl = bannerUrl,
        rating = if (rating <= 0) 4.8 else rating,
        views = views,
        likes = likes,
        releaseYear = if (releaseYear <= 0) 2024 else releaseYear,
        director = director,
        cast = cast,
        totalEpisodes = if (totalEpisodes > 0) totalEpisodes else episodes.size,
        isTrending = isTrending,
        isTop10 = isTop10,
        topRank = topRank,
        tags = tags,
        episodes = episodes.map { it.toDomain() }
    )
}

fun EpisodeFirestoreDto.toDomain(): Episode {
    return Episode(
        id = id,
        dramaId = dramaId,
        episodeNumber = episodeNumber,
        title = title,
        durationSeconds = durationSeconds,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        synopsis = synopsis,
        isFree = isFree,
        likesCount = likesCount
    )
}

fun Drama.toFirestoreDto(): DramaFirestoreDto {
    return DramaFirestoreDto(
        id = id,
        title = title,
        originalTitle = originalTitle,
        synopsis = synopsis,
        category = category.name,
        posterUrl = posterUrl,
        bannerUrl = bannerUrl,
        rating = rating,
        views = views,
        likes = likes,
        releaseYear = releaseYear,
        director = director,
        cast = cast,
        totalEpisodes = totalEpisodes,
        isTrending = isTrending,
        isTop10 = isTop10,
        topRank = topRank,
        tags = tags,
        episodes = episodes.map { it.toFirestoreDto() }
    )
}

fun Episode.toFirestoreDto(): EpisodeFirestoreDto {
    return EpisodeFirestoreDto(
        id = id,
        dramaId = dramaId,
        episodeNumber = episodeNumber,
        title = title,
        durationSeconds = durationSeconds,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        synopsis = synopsis,
        isFree = isFree,
        likesCount = likesCount
    )
}

/**
 * Remote Data Source backed by Firebase Firestore for real-time drama and episode metadata sync.
 */
class FirestoreDramaDataSource {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "FirebaseFirestore not initialized or unavailable: ${e.message}")
            null
        }
    }

    /**
     * Real-time stream of all dramas and their episodes from the 'dramas' collection in Firestore.
     */
    fun observeDramas(): Flow<List<Drama>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection("dramas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreDramaDataSource", "Listen failed on dramas collection", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(DramaFirestoreDto::class.java)
                            dto?.copy(id = doc.id)?.toDomain()
                        } catch (e: Exception) {
                            Log.e("FirestoreDramaDataSource", "Error parsing drama doc ${doc.id}", e)
                            null
                        }
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Fetch catalog once from Firestore.
     */
    suspend fun fetchDramasOnce(): List<Drama> {
        val db = firestore ?: return emptyList()
        return try {
            val snapshot = db.collection("dramas").get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents.mapNotNull { doc ->
                    val dto = doc.toObject(DramaFirestoreDto::class.java)
                    dto?.copy(id = doc.id)?.toDomain()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Error fetching dramas from Firestore", e)
            emptyList()
        }
    }

    /**
     * Seeds initial curated drama catalog into Firestore if collection is empty or upon request.
     */
    suspend fun seedCatalog(dramas: List<Drama>) {
        val db = firestore ?: return
        try {
            for (drama in dramas) {
                val dto = drama.toFirestoreDto()
                db.collection("dramas").document(drama.id)
                    .set(dto, SetOptions.merge())
                    .await()
            }
            Log.d("FirestoreDramaDataSource", "Successfully seeded ${dramas.size} dramas into Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Failed to seed Firestore data", e)
        }
    }

    /**
     * Increment views count in real time in Firestore.
     */
    fun incrementDramaViews(dramaId: String) {
        val db = firestore ?: return
        try {
            db.collection("dramas").document(dramaId)
                .update("views", FieldValue.increment(1))
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Failed to increment views for $dramaId", e)
        }
    }

    /**
     * Increment or decrement likes count in Firestore.
     */
    fun updateDramaLikes(dramaId: String, increment: Long) {
        val db = firestore ?: return
        try {
            db.collection("dramas").document(dramaId)
                .update("likes", FieldValue.increment(increment))
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Failed to update likes for $dramaId", e)
        }
    }
}
