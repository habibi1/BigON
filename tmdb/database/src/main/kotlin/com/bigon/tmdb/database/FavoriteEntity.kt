package com.bigon.tmdb.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A favourited movie, stored as a self-contained snapshot rather than a
 * reference into [MovieEntity]: category rows are transient (each refresh
 * replaces its list), while favourites must survive cache eviction, cache
 * clearing and being fully offline. Genre names are resolved at save time for
 * the same reason — no join against a table that might be empty later.
 */
@Entity(tableName = "favorite_entity")
data class FavoriteEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val overview: String,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    @ColumnInfo(name = "backdrop_url") val backdropUrl: String?,
    @ColumnInfo(name = "release_date") val releaseDate: String?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double?,
    @ColumnInfo(name = "genres") val genreNames: List<String>,
    /** Insertion order drives the Favorites screen (newest first). */
    @ColumnInfo(name = "added_at") val addedAt: Long,
    /**
     * When the TMDB-derived fields above were last fetched — distinct from
     * [addedAt], which records the user's action and must never move. Refreshing
     * a snapshot would otherwise reorder the Favorites screen under the reader.
     *
     * Drives the six-month refresh required by TMDB's terms.
     */
    @ColumnInfo(name = "snapshot_at") val snapshotAt: Long,
)
