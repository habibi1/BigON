package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/trending/all/week` — films, series and people in one list, distinguished by
 * `media_type`.
 *
 * Modelled as one flat DTO with everything optional rather than a
 * kotlinx.serialization polymorphic hierarchy. The wire format is not a sealed
 * hierarchy: TMDB does not emit a stable discriminator on nested objects, adds
 * fields per type without warning, and has been observed returning types the
 * documentation does not list. A permissive DTO absorbs all of that, and the
 * narrowing happens once, in the mapper, where an unknown type is dropped
 * rather than thrown.
 */
@Serializable
data class TrendingListResponse(
    val page: Int = 1,
    val results: List<TrendingItemDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class TrendingItemDto(
    val id: Long = 0,
    @SerialName("media_type") val mediaType: String = "",

    // film
    val title: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,

    // series
    val name: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,

    // film + series
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),

    // person
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("known_for") val knownFor: List<TrendingItemDto> = emptyList(),
)

/** `/watch/providers/regions` — the regions TMDB actually holds data for. */
@Serializable
data class RegionListResponse(
    val results: List<RegionDto> = emptyList(),
)

@Serializable
data class RegionDto(
    @SerialName("iso_3166_1") val code: String = "",
    @SerialName("english_name") val englishName: String = "",
    @SerialName("native_name") val nativeName: String = "",
)
