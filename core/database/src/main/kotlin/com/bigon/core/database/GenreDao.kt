package com.bigon.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {

    @Query("SELECT * FROM genre_entity")
    fun observeAll(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genre_entity")
    suspend fun getAll(): List<GenreEntity>

    @Query("SELECT COUNT(*) FROM genre_entity")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(genres: List<GenreEntity>)
}
