package com.bigon.core.model

import java.time.LocalDate

/**
 * A series. Deliberately a sibling of [MovieDetail] rather than a shared
 * supertype: they overlap on about half their fields and diverge on the half
 * that matters — a series has seasons, episode counts and a running status,
 * a film has a runtime and a release date. Forcing one type would make every
 * field optional and push "which of these apply?" onto every call site.
 */
data class TvDetail(
    val id: Long,
    val name: String,
    val tagline: String?,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val firstAirDate: LocalDate?,
    val lastAirDate: LocalDate?,
    val voteAverage: Double?,
    val voteCount: Int,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    /** "Returning Series", "Ended", "Canceled" — TMDB's own vocabulary. */
    val status: String?,
    val genres: List<String>,
    val cast: List<CastMember>,
    val trailerKey: String?,
    /** Age rating for the resolved region, from `content_ratings`. */
    val certification: String?,
    val watchProviders: WatchProviders?,
) {
    val firstAirYear: Int? get() = firstAirDate?.year

    /** "2011 – 2019" for an ended series, "2011 –" while it is still running. */
    val airedRange: String?
        get() = firstAirDate?.let { start ->
            val end = lastAirDate?.year?.takeIf { status?.equals("Ended", true) == true || status?.equals("Canceled", true) == true }
            if (end != null) "${start.year} – $end" else "${start.year} –"
        }
}
