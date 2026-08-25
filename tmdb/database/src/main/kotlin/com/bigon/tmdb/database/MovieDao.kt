package com.bigon.tmdb.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Reads are cold [Flow]s; writes are main-safe suspend functions — Room owns the
 * threading. Every query is scoped by `list_key` so categories stay independent.
 */
@Dao
interface MovieDao {

    @Query("SELECT * FROM movie_entity WHERE list_key = :listKey ORDER BY position ASC")
    fun observeByList(listKey: String): Flow<List<MovieEntity>>

    /**
     * A movie legitimately has one row per list it appears in, so this matches
     * several. `rowid` descending picks the most recently written of them:
     * [replaceList] deletes and reinserts a whole category, so the newest rowid
     * is the freshest fetch. Without the ordering SQLite returns whichever row
     * it scans first — in practice the oldest, which is the one most likely to
     * be carrying artwork TMDB has since replaced.
     */
    @Query("SELECT * FROM movie_entity WHERE id = :id ORDER BY rowid DESC LIMIT 1")
    fun observeById(id: Long): Flow<MovieEntity?>

    /**
     * IGNORE, not REPLACE: TMDB occasionally repeats a movie on adjacent pages,
     * and replacing would move the earlier row to the later position —
     * a silent reorder. First sighting wins.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(movies: List<MovieEntity>)

    @Query("SELECT MAX(page) FROM movie_entity WHERE list_key = :listKey")
    suspend fun maxPage(listKey: String): Int?

    @Query("SELECT MAX(position) FROM movie_entity WHERE list_key = :listKey")
    suspend fun maxPosition(listKey: String): Int?

    @Query("DELETE FROM movie_entity WHERE list_key = :listKey")
    suspend fun deleteList(listKey: String)

    /** Wipes the whole catalogue cache. Favourites live in their own table. */
    @Query("DELETE FROM movie_entity")
    suspend fun clearAll()

    /** Replaces exactly one category, leaving every other cached list intact. */
    @Transaction
    suspend fun replaceList(listKey: String, movies: List<MovieEntity>) {
        deleteList(listKey)
        insertIgnoringDuplicates(movies)
    }

    /** Appends a page below the existing rows; duplicates are dropped. */
    @Transaction
    suspend fun appendList(movies: List<MovieEntity>) {
        insertIgnoringDuplicates(movies)
    }
}
