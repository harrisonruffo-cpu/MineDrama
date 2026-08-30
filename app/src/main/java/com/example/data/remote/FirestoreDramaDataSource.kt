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
    val episodes: List<EpisodeFirestoreDto> = emptyList(),
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = ""
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
    val likesCount: Long = 0L,
    val uploadedAt: Long = 0L
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
        bannerUrl = bannerUrl.ifBlank { posterUrl },
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
        episodes = episodes.map { it.toDomain() },
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl
    )
}

fun EpisodeFirestoreDto.toDomain(): Episode {
    return Episode(
        id = id.ifBlank { "${dramaId}_ep_$episodeNumber" },
        dramaId = dramaId,
        episodeNumber = episodeNumber,
        title = title,
        durationSeconds = durationSeconds,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        synopsis = synopsis,
        isFree = isFree,
        likesCount = likesCount,
        uploadedAt = if (uploadedAt <= 0) System.currentTimeMillis() else uploadedAt
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
        episodes = episodes.map { it.toFirestoreDto() },
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl
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
        likesCount = likesCount,
        uploadedAt = uploadedAt
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
     * Publish or update drama and its episodes in Firestore.
     */
    suspend fun publishOrUpdateDrama(drama: Drama): Boolean {
        val db = firestore ?: return false
        return try {
            val dto = drama.toFirestoreDto()
            db.collection("dramas").document(drama.id)
                .set(dto, SetOptions.merge())
                .await()
            Log.d("FirestoreDramaDataSource", "Successfully saved drama ${drama.id} - ${drama.title}")
            true
        } catch (e: Exception) {
            Log.e("FirestoreDramaDataSource", "Error saving drama ${drama.id}", e)
            false
        }
    }

    /**
     * Delete drama from Firestore.
     */
    suspend fun deleteDrama(dramaId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("dramas").document(dramaId).delete().await()
            Log.d("FirestoreDramaDataSource", "Successfully deleted drama $dramaId")
            true
        } catch (e: Exception) {
            Log.e("FirestoreDramaDataSource", "Error deleting drama $dramaId", e)
            false
        }
    }

    /**
     * Rename an episode in a drama.
     */
    suspend fun renameEpisode(dramaId: String, episodeId: String, newTitle: String): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection("dramas").document(dramaId)
            val snapshot = docRef.get().await()
            val dto = snapshot.toObject(DramaFirestoreDto::class.java) ?: return false
            val updatedEpisodes = dto.episodes.map { ep ->
                if (ep.id == episodeId) ep.copy(title = newTitle) else ep
            }
            docRef.update("episodes", updatedEpisodes).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreDramaDataSource", "Error renaming episode $episodeId", e)
            false
        }
    }

    /**
     * Rename a drama title.
     */
    suspend fun renameDrama(dramaId: String, newTitle: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("dramas").document(dramaId).update("title", newTitle).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreDramaDataSource", "Error renaming drama $dramaId", e)
            false
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

    /**
     * Save user profile to Firestore cloud.
     */
    suspend fun saveUserProfile(user: com.example.data.auth.UserProfile): Boolean {
        val db = firestore ?: return false
        return try {
            val data = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "photoUrl" to user.photoUrl,
                "isCreator" to user.isCreator,
                "lastActive" to System.currentTimeMillis()
            )
            db.collection("users").document(user.uid)
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Failed to save user profile to cloud: ${e.message}")
            false
        }
    }

    /**
     * Fetch user profile from Firestore cloud.
     */
    suspend fun getUserProfile(uid: String): com.example.data.auth.UserProfile? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                com.example.data.auth.UserProfile(
                    uid = doc.getString("uid") ?: uid,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    isAnonymous = false,
                    isCreator = doc.getBoolean("isCreator") ?: true
                )
            } else null
        } catch (e: Exception) {
            Log.w("FirestoreDramaDataSource", "Failed to get user profile from cloud: ${e.message}")
            null
        }
    }
}
