package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Drama(
    val id: String,
    val title: String,
    val originalTitle: String = "",
    val synopsis: String,
    val category: DramaCategory,
    val posterUrl: String,
    val bannerUrl: String,
    val rating: Double,
    val views: Long,
    val likes: Long,
    val releaseYear: Int,
    val director: String = "Direção Mine Drama",
    val cast: List<String> = emptyList(),
    val totalEpisodes: Int,
    val isTrending: Boolean = false,
    val isTop10: Boolean = false,
    val topRank: Int? = null,
    val tags: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class Episode(
    val id: String,
    val dramaId: String,
    val episodeNumber: Int,
    val title: String,
    val durationSeconds: Int,
    val videoUrl: String,
    val thumbnailUrl: String,
    val synopsis: String = "",
    val isFree: Boolean = true,
    val likesCount: Long = 0,
    val uploadedAt: Long = System.currentTimeMillis()
)

enum class DramaCategory(val displayName: String, val iconEmoji: String) {
    TODAS("Todas", "✨"),
    EM_ALTA("Em Alta", "🔥"),
    ROMANCE_CEO("Romance de CEO", "💼"),
    VINGANCA("Vingança & Poder", "⚡"),
    AMOR_PROIBIDO("Amor Proibido", "🌹"),
    HISTORICO("Histórico & Fantasia", "👑"),
    SUSPENSE("Suspense & Mistério", "🔍"),
    COMEDIA("Comédia Romântica", "💖")
}

data class PlaybackEpisodeItem(
    val drama: Drama,
    val episode: Episode,
    val episodeIndex: Int,
    val totalEpisodes: Int
)
