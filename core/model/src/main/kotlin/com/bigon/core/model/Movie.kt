package com.bigon.core.model

import java.time.LocalDate

/**
 * Cross-feature domain entity. Layer boundaries exchange immutable data classes
 * like this one — never DTOs, Bundles, or SDK types.
 *
 * Deliberately free of TMDB's shape: genres arrive as resolved names rather than
 * ids, images as ready-to-load URLs rather than path fragments, and an unrated
 * title carries a null [voteAverage] rather than TMDB's `0.0`. Performing those
 * translations is the data layer's job, so no consumer needs to know how TMDB
 * encodes them.
 */
data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: LocalDate?,
    /** Null when nobody has rated the title yet. */
    val voteAverage: Double?,
    val genres: List<String> = emptyList(),
) {
    val releaseYear: Int? get() = releaseDate?.year
}
