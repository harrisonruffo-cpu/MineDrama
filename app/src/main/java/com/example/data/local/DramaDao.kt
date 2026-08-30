package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {
    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE dramaId = :dramaId LIMIT 1")
    suspend fun getWatchHistory(dramaId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE dramaId = :dramaId")
    suspend fun deleteWatchHistory(dramaId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE dramaId = :dramaId)")
    fun isFavoriteFlow(dramaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE dramaId = :dramaId)")
    suspend fun isFavorite(dramaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE dramaId = :dramaId")
    suspend fun removeFavorite(dramaId: String)

    // Liked Episodes
    @Query("SELECT compositeKey FROM liked_episodes")
    fun getAllLikedKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_episodes WHERE dramaId = :dramaId AND episodeNumber = :episodeNumber)")
    fun isEpisodeLikedFlow(dramaId: String, episodeNumber: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedEpisode(liked: LikedEpisodeEntity)

    @Query("DELETE FROM liked_episodes WHERE dramaId = :dramaId AND episodeNumber = :episodeNumber")
    suspend fun removeLikedEpisode(dramaId: String, episodeNumber: Int)
}
