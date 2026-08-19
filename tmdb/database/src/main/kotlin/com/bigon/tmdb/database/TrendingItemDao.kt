package com.bigon.tmdb.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrendingItemDao {

    @Query("SELECT * FROM trending_item_entity ORDER BY position ASC")
    fun observeAll(): Flow<List<TrendingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrendingItemEntity>)

    @Query("DELETE FROM trending_item_entity")
    suspend fun clear()

    /**
     * The feed is a ranking, so a refresh replaces it wholesale — merging would
     * leave last week's entries interleaved with this week's at stale
     * positions.
     */
    @Transaction
    suspend fun replaceAll(items: List<TrendingItemEntity>) {
        clear()
        insertAll(items)
    }
}
