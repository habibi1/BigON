package com.bigon.tmdb.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Persistence row for a movie inside one category list.
 *
 * The primary key is composite: the same movie legitimately appears in several
 * lists (trending *and* popular), and each needs its own cached position. This
 * is what stops refreshing one category from evicting another.
 *
 * [position] preserves the server's ordering, which is meaningful for every TMDB
 * list and is not recoverable from any other column.
 */
@Entity(
    tableName = "movie_entity",
    primaryKeys = ["id", "list_key"],
    indices = [Index("list_key")],
)
data class MovieEntity(
    val id: Long,
    @ColumnInfo(name = "list_key") val listKey: String,
    /** TMDB page this row arrived on; drives append pagination. */
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "position") val position: Int,
    val title: String,
    val overview: String,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    /** TMDB sends `""` for unknown dates; stored as null after normalisation. */
    @ColumnInfo(name = "release_date") val releaseDate: String?,
    /** Null when unrated — TMDB's `0.0` placeholder is normalised away. */
    @ColumnInfo(name = "vote_average") val voteAverage: Double?,
    /** Genre ids; names are resolved by joining against [GenreEntity]. */
    @ColumnInfo(name = "genre_ids") val genreIds: List<Int>,
)
