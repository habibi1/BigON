package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/movie/{id}?append_to_response=credits,videos` — one request instead of
 * three. The appended blocks arrive as nested objects on the same payload.
 */
@Serializable
data class MovieDetailResponse(
    val id: Long,
    val title: String = "",
    val tagline: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    /** Null or 0 for unreleased titles. */
    val runtime: Int? = null,
    val genres: List<GenreDto> = emptyList(),
    val credits: CreditsDto = CreditsDto(),
    val videos: VideosDto = VideosDto(),
)

@Serializable
data class CreditsDto(
    val cast: List<CastDto> = emptyList(),
)

@Serializable
data class CastDto(
    val id: Long = 0,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 0,
)

@Serializable
data class VideosDto(
    val results: List<VideoDto> = emptyList(),
)

@Serializable
data class VideoDto(
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)
