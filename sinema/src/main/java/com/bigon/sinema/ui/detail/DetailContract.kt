package com.bigon.sinema.ui.detail

import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieDetail
import com.bigon.tmdb.model.Review
import com.bigon.tmdb.model.WatchProviders
import com.bigon.core.ui.UiText

/**
 * Detail paints in two passes: [cached] arrives instantly from Room so the
 * shared-element transition lands on real content, then [detail] fills in the
 * parts only the network has (runtime, cast, trailer).
 */
data class DetailUiState(
    val movieId: Long,
    val cached: Movie? = null,
    val detail: MovieDetail? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    /**
     * Reviews arrive from their own request, so they carry their own loading
     * state instead of riding [isLoading]: a review failure must not blank
     * detail content that loaded fine.
     */
    val reviews: List<Review> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val reviewsError: UiText? = null,
    val reviewsPage: Int = 0,
    val hasMoreReviews: Boolean = false,
    val totalReviews: Int = 0,
    /**
     * The artwork actually on screen, pinned to whatever loaded first and then
     * left alone for the life of the screen.
     *
     * Two sources feed this and they disagree more often than they look like
     * they should. The cached row was written when its list was last fetched;
     * the detail response is fetched now. TMDB replaces artwork continuously,
     * and its list endpoints serve a cached payload that lags detail — both
     * have been observed live for the same film on the same day. Repainting on
     * every disagreement means the hero image visibly changes under the reader
     * a moment after the screen opens, and swapping one valid backdrop for
     * another buys nothing.
     *
     * The fresher value is not discarded: the repository writes it back over
     * the cached row, so the correction shows up the next time this film is
     * opened rather than as a flash in front of the user.
     */
    val paintedPosterUrl: String? = null,
    val paintedBackdropUrl: String? = null,
) {
    val title: String? get() = detail?.title ?: cached?.title

    /**
     * The freshest artwork known, which is what a favourite should be saved
     * with — [paintedPosterUrl] is what is rendered.
     */
    val posterUrl: String? get() = detail?.posterUrl ?: cached?.posterUrl
    val backdropUrl: String? get() = detail?.backdropUrl ?: cached?.backdropUrl
    val overview: String? get() = (detail?.overview ?: cached?.overview)?.takeIf { it.isNotBlank() }
    val rating: Double? get() = detail?.voteAverage ?: cached?.voteAverage
    val year: Int? get() = detail?.releaseYear ?: cached?.releaseYear
    val genres: List<String> get() = detail?.genres ?: cached?.genres.orEmpty()

    /** Detail-only fields: absent on the first paint, and often absent entirely. */
    val certification: String? get() = detail?.certification?.takeIf { it.isNotBlank() }
    val keywords: List<String> get() = detail?.keywords.orEmpty()
    val recommendations: List<Movie> get() = detail?.recommendations.orEmpty()

    /** Null when TMDB has no availability for the resolved region, which is common. */
    val watchProviders: WatchProviders? get() = detail?.watchProviders

    val logoUrl: String? get() = detail?.logoUrl
    val alternativeTitles: List<String> get() = detail?.alternativeTitles.orEmpty()

    /** The overview is in the device language, though the app's UI is not. */
    val isLocalised: Boolean get() = detail?.isLocalised == true

    /**
     * The section appears once there is something to say — reviews, a spinner,
     * or an error. A title with no reviews shows nothing rather than an empty
     * heading, which is the common case for unreleased films.
     */
    val showReviews: Boolean
        get() = reviews.isNotEmpty() || isLoadingReviews || reviewsError != null

    /**
     * The network response has not arrived and is still expected, so every
     * section that only it can fill should be showing a skeleton.
     *
     * Deliberately not tied to [cached]. Arriving from a list, the cached row
     * supplies a title, a poster and an overview immediately while runtime,
     * cast, availability and recommendations are still in flight — and those
     * used to appear with no warning that they were coming. Pending is about
     * the response, not about whether the screen looks empty.
     *
     * An error clears it: a skeleton that never resolves is a screen claiming
     * to be busy when nothing is happening, and the retry sits above it.
     */
    val isDetailPending: Boolean get() = detail == null && error == null

    /**
     * The snapshot written when the user favourites from this screen. Null until
     * something loaded — the heart is disabled until then.
     */
    fun asMovie(): Movie? = title?.let { resolvedTitle ->
        Movie(
            id = movieId,
            title = resolvedTitle,
            overview = overview.orEmpty(),
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            releaseDate = detail?.releaseDate ?: cached?.releaseDate,
            voteAverage = rating,
            genres = genres,
        )
    }
}

sealed interface DetailIntent {
    data object Retry : DetailIntent
    data object Back : DetailIntent
    data class FavoriteToggled(val favorite: Boolean) : DetailIntent
    /** A recommendation was tapped; the shell decides what opening it means. */
    data class RecommendationClicked(val movieId: Long) : DetailIntent
    /** Retrying a failed review load. */
    data object ReviewsRequested : DetailIntent
    data object MoreReviewsRequested : DetailIntent

    /**
     * Nothing on the device can open the trailer — no YouTube, no browser. The
     * button stays enabled because a key exists; what is missing is somewhere
     * to send it.
     */
    data object TrailerUnavailable : DetailIntent
}
