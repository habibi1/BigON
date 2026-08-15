package com.bigon.tmdb.domain.movie

/**
 * The refinements TMDB's discover endpoint accepts beyond genre and provider.
 *
 * A value class bundle rather than a widening parameter list: discover already
 * takes genre, page and provider, and adding four more positional arguments
 * would make every call site unreadable and every new filter a breaking change.
 */
data class DiscoverFilters(
    val sort: DiscoverSort = DiscoverSort.Popularity,
    val releaseYear: Int? = null,
    val minRating: Double? = null,
    val maxRuntimeMinutes: Int? = null,
) {
    val isActive: Boolean
        get() = sort != DiscoverSort.Popularity ||
            releaseYear != null ||
            minRating != null ||
            maxRuntimeMinutes != null

    /** How many refinements are on, for a badge on the filter affordance. */
    val activeCount: Int
        get() = listOfNotNull(
            sort.takeIf { it != DiscoverSort.Popularity },
            releaseYear,
            minRating,
            maxRuntimeMinutes,
        ).size

    companion object {
        /**
         * A rating floor without a vote floor is meaningless — one 10/10 vote
         * would outrank a thousand good ones — so this rides along whenever
         * [minRating] is set.
         */
        const val MIN_VOTES_FOR_RATING_FILTER = 200
    }
}

enum class DiscoverSort(val label: String, val apiValue: String) {
    Popularity("Popular", "popularity.desc"),
    Rating("Top rated", "vote_average.desc"),
    Newest("Newest", "primary_release_date.desc"),
    Revenue("Box office", "revenue.desc"),
}
