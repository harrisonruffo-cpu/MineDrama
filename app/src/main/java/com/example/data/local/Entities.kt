package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val dramaId: String,
    val dramaTitle: String,
    val posterUrl: String,
    val lastEpisodeNumber: Int,
    val lastPositionMs: Long,
    val durationMs: Long,
    val totalEpisodes: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val dramaId: String,
    val dramaTitle: String,
    val posterUrl: String,
    val categoryName: String,
    val rating: Double,
    val totalEpisodes: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_episodes")
data class LikedEpisodeEntity(
    @PrimaryKey
    val compositeKey: String, // dramaId_episodeNumber
    val dramaId: String,
    val episodeNumber: Int,
    val likedAt: Long = System.currentTimeMillis()
)
