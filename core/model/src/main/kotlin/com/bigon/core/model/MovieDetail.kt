package com.bigon.core.model

import java.time.LocalDate

/**
 * The full record behind a single title. Richer than [Movie], which is the
 * list-shaped projection — detail adds runtime, tagline, cast and a trailer.
 */
data class MovieDetail(
    val id: Long,
    val title: String,
    val tagline: String?,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: LocalDate?,
    val voteAverage: Double?,
    val voteCount: Int,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val cast: List<CastMember>,
    /** YouTube key for the trailer, when TMDB has one. */
    val trailerKey: String?,
) {
    val releaseYear: Int? get() = releaseDate?.year
}

data class CastMember(
    val id: Long,
    val name: String,
    val character: String,
    val profileUrl: String?,
)
