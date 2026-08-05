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
    /**
     * TMDB's own recommendations, already merged with `similar` as a fallback:
     * recommendations are editorially better but thin or absent for obscure
     * titles, where `similar` (genre/keyword derived) still returns something.
     */
    val recommendations: List<Movie> = emptyList(),
    /** Age rating for the resolved region, e.g. "PG-13". Absent for many titles. */
    val certification: String? = null,
    val keywords: List<String> = emptyList(),
    val imdbId: String? = null,
    val watchProviders: WatchProviders? = null,
    /**
     * TMDB's title treatment — the stylised wordmark studios ship with the
     * artwork. Shown over the backdrop in place of setting the title in the
     * app's own typeface, which never matches the film's identity.
     */
    val logoUrl: String? = null,
    /** "Also known as" — release titles from other markets, deduplicated. */
    val alternativeTitles: List<String> = emptyList(),
    /**
     * True when [overview]/[tagline] came from a TMDB translation for the
     * device language rather than the default English copy. Surfaced so the UI
     * can be honest about it rather than implying the app itself is localised.
     */
    val isLocalised: Boolean = false,
) {
    val releaseYear: Int? get() = releaseDate?.year

    val imdbUrl: String? get() = imdbId?.takeIf { it.isNotBlank() }?.let { "https://www.imdb.com/title/$it/" }
}

data class CastMember(
    val id: Long,
    val name: String,
    val character: String,
    val profileUrl: String?,
)

/**
 * Where a title can be watched in one region. TMDB licenses this from
 * JustWatch, whose terms require linking back rather than presenting the data
 * as our own — hence [link] is not optional in practice.
 */
data class WatchProviders(
    val region: String,
    val link: String?,
    val streaming: List<WatchProvider>,
    val rent: List<WatchProvider>,
    val buy: List<WatchProvider>,
) {
    val isEmpty: Boolean get() = streaming.isEmpty() && rent.isEmpty() && buy.isEmpty()
}

data class WatchProvider(
    val id: Int,
    val name: String,
    val logoUrl: String?,
)
