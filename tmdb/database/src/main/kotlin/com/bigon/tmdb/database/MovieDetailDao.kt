package com.bigon.tmdb.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MovieDetailDao {

    @Query("SELECT * FROM movie_detail_entity WHERE id = :movieId")
    suspend fun byId(movieId: Long): MovieDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MovieDetailEntity)

    @Query("DELETE FROM movie_detail_entity")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM movie_detail_entity")
    suspend fun count(): Int

    /**
     * Keeps the cache bounded. Detail payloads are an order of magnitude larger
     * than list rows, so an unbounded cache would grow without any signal to
     * the user beyond a slowly swelling storage figure.
     */
    @Query(
        """
        DELETE FROM movie_detail_entity
        WHERE id NOT IN (
            SELECT id FROM movie_detail_entity ORDER BY fetched_at DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimTo(keep: Int)

    /**
     * Drops payloads cached before [cutoff]. TMDB's terms cap how long their
     * content may be held, so this is an obligation rather than housekeeping:
     * [trimTo] bounds the cache by size, this bounds it by age, and the two are
     * independent — fewer than `keep` rows still expire.
     */
    @Query("DELETE FROM movie_detail_entity WHERE fetched_at < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long)

    @Transaction
    suspend fun save(entity: MovieDetailEntity, keep: Int, cutoff: Long) {
        upsert(entity)
        purgeOlderThan(cutoff)
        trimTo(keep)
    }
}
