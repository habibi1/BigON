package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── collections ─────────────────────────────────────────────────────────────

/**
 * `/collection/{id}`. Small — the Iron Man collection is 2 KB for three parts —
 * because `parts` reuses the list shape rather than embedding full details.
 */
@Serializable
data class CollectionResponse(
    val id: Long = 0,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val parts: List<MovieDto> = emptyList(),
)

/** The stub already present on a movie's detail payload. */
@Serializable
data class CollectionRefDto(
    val id: Long = 0,
    val name: String = "",
)

// ── people ──────────────────────────────────────────────────────────────────

/**
 * `/person/{id}?append_to_response=movie_credits` — ~92 KB, of which the
 * credits are nearly all of it (95 cast + 29 crew for a working actor).
 */
@Serializable
data class PersonResponse(
    val id: Long = 0,
    val name: String = "",
    val biography: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("movie_credits") val movieCredits: PersonCreditsDto = PersonCreditsDto(),
)

@Serializable
data class PersonCreditsDto(
    val cast: List<PersonCastCreditDto> = emptyList(),
    val crew: List<PersonCrewCreditDto> = emptyList(),
)

@Serializable
data class PersonCastCreditDto(
    val id: Long = 0,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val character: String = "",
)

@Serializable
data class PersonCrewCreditDto(
    val id: Long = 0,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val job: String = "",
)

// ── tv ──────────────────────────────────────────────────────────────────────

/**
 * `/tv/{id}` with the same block strategy as movie detail. `content_ratings` is
 * TV's equivalent of `release_dates` — a different key for the same idea, which
 * is exactly the sort of divergence that makes a shared movie/TV type a poor
 * fit.
 */
@Serializable
data class TvDetailResponse(
    val id: Long = 0,
    val name: String = "",
    val tagline: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    val status: String? = null,
    val genres: List<GenreDto> = emptyList(),
    val credits: CreditsDto = CreditsDto(),
    val videos: VideosDto = VideosDto(),
    @SerialName("content_ratings") val contentRatings: ContentRatingsDto = ContentRatingsDto(),
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto = WatchProvidersDto(),
)

@Serializable
data class ContentRatingsDto(
    val results: List<ContentRatingDto> = emptyList(),
)

@Serializable
data class ContentRatingDto(
    @SerialName("iso_3166_1") val country: String = "",
    val rating: String = "",
)
