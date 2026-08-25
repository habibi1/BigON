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

    /**
     * The stored snapshot, or null when the movie is not favourited. Used to
     * refresh a snapshot in place without ever creating one — a film the user
     * has not favourited must not become a favourite just by being opened.
     */
    @Query("SELECT * FROM favorite_entity WHERE id = :movieId")
    suspend fun byId(movieId: Long): FavoriteEntity?

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_entity WHERE id = :movieId")
    suspend fun delete(movieId: Long)
}
