package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/movie/{id}` with appended blocks — one request instead of eight. The
 * appended blocks arrive as nested objects on the same payload.
 *
 * Which blocks are appended is a byte-budget decision, not a free one — see
 * [MovieApi.detail] for the measured cost of each and why `images` is scoped
 * to one language while `reviews` has its own endpoint.
 *
 * Every field is defaulted because TMDB omits blocks entirely for sparse
 * titles rather than returning empty ones.
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
    val recommendations: MovieListResponse? = null,
    val similar: MovieListResponse? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesDto = ReleaseDatesDto(),
    val keywords: KeywordsDto = KeywordsDto(),
    @SerialName("external_ids") val externalIds: ExternalIdsDto = ExternalIdsDto(),
    /**
     * The only TMDB block whose JSON key is not a valid Kotlin identifier —
     * hence the explicit name rather than the property-name default.
     */
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto = WatchProvidersDto(),
    val images: ImagesDto = ImagesDto(),
    @SerialName("alternative_titles") val alternativeTitles: AlternativeTitlesDto = AlternativeTitlesDto(),
    val translations: TranslationsDto = TranslationsDto(),
    /** Already on the payload — no extra request to know a film is part of one. */
    @SerialName("belongs_to_collection") val belongsToCollection: CollectionRefDto? = null,
)

/**
 * Artwork. Only [logos] is read — the title treatment shown over the backdrop.
 * Posters and backdrops arrive regardless and are ignored: the list endpoints
 * already supply those paths, and re-picking them here would fight the sizes
 * the design system asks for.
 */
@Serializable
data class ImagesDto(
    val logos: List<ImageDto> = emptyList(),
)

@Serializable
data class ImageDto(
    @SerialName("file_path") val filePath: String = "",
    @SerialName("iso_639_1") val language: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class AlternativeTitlesDto(
    val titles: List<AlternativeTitleDto> = emptyList(),
)

@Serializable
data class AlternativeTitleDto(
    @SerialName("iso_3166_1") val country: String = "",
    val title: String = "",
    val type: String = "",
)

/**
 * Localised copy. ~55 languages arrive and one is used, for the same reason as
 * the other region-scoped blocks: appended blocks take no server-side filter.
 */
@Serializable
data class TranslationsDto(
    val translations: List<TranslationDto> = emptyList(),
)

@Serializable
data class TranslationDto(
    @SerialName("iso_639_1") val language: String = "",
    @SerialName("iso_3166_1") val country: String = "",
    val data: TranslationDataDto = TranslationDataDto(),
)

@Serializable
data class TranslationDataDto(
    val title: String = "",
    val overview: String = "",
    val tagline: String = "",
)

/**
 * Certifications are nested per country. TMDB returns ~85 of them and the app
 * shows one, but there is no server-side filter for an appended block.
 */
@Serializable
data class ReleaseDatesDto(
    val results: List<ReleaseDateCountryDto> = emptyList(),
)

@Serializable
data class ReleaseDateCountryDto(
    @SerialName("iso_3166_1") val country: String = "",
    @SerialName("release_dates") val releases: List<ReleaseDateDto> = emptyList(),
)

@Serializable
data class ReleaseDateDto(
    val certification: String = "",
    /** 3 = theatrical, the type whose certification users recognise. */
    val type: Int = 0,
)

@Serializable
data class KeywordsDto(
    val keywords: List<KeywordDto> = emptyList(),
)

@Serializable
data class KeywordDto(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class ExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
data class WatchProvidersDto(
    /** Keyed by ISO-3166-1 country code. ~98 regions arrive; one is displayed. */
    val results: Map<String, WatchProviderCountryDto> = emptyMap(),
)

@Serializable
data class WatchProviderCountryDto(
    /** TMDB's deep link into its own "where to watch" page for the region. */
    val link: String? = null,
    /** Subscription streaming. */
    val flatrate: List<ProviderDto> = emptyList(),
    val rent: List<ProviderDto> = emptyList(),
    val buy: List<ProviderDto> = emptyList(),
)

@Serializable
data class ProviderDto(
    @SerialName("provider_id") val providerId: Int = 0,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = 0,
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

/**
 * `/watch/providers/movie` — the catalogue of services carrying films in one
 * region, as opposed to the per-title availability nested in detail.
 *
 * The count varies enormously by market: 289 services in the US, 46 in
 * Indonesia. A UI that assumed a handful would be wrong in both directions,
 * which is why the filter row takes the top few by TMDB's own display
 * priority rather than showing everything.
 */
@Serializable
data class WatchProviderListResponse(
    val results: List<ProviderDto> = emptyList(),
)
