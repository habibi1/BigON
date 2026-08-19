package com.bigon.tmdb.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_entity ORDER BY added_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_entity WHERE id = :movieId)")
    fun observeIsFavorite(movieId: Long): Flow<Boolean>

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_entity WHERE id = :movieId")
    suspend fun delete(movieId: Long)
}
