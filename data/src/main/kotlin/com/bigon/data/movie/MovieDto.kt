package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a TMDB paginated list. */
@Serializable
data class MovieListResponse(
    val page: Int = 1,
    val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

/**
 * A list item as TMDB sends it. Every quirk is preserved verbatim here and
 * normalised in [MovieMapper] — DTOs mirror the wire, never the domain.
 */
@Serializable
data class MovieDto(
    val id: Long,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    /** Empty string, not null, when TMDB has no date. */
    @SerialName("release_date") val releaseDate: String? = null,
    /** `0.0` when nobody has rated the title. */
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class GenreListResponse(
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String = "",
)
