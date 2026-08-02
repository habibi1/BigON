package com.bigon.sinema.ui.detail

import com.bigon.core.model.Movie
import com.bigon.core.model.MovieDetail
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
) {
    val title: String? get() = detail?.title ?: cached?.title
    val posterUrl: String? get() = detail?.posterUrl ?: cached?.posterUrl
    val backdropUrl: String? get() = detail?.backdropUrl ?: cached?.backdropUrl
    val overview: String? get() = (detail?.overview ?: cached?.overview)?.takeIf { it.isNotBlank() }
    val rating: Double? get() = detail?.voteAverage ?: cached?.voteAverage
    val year: Int? get() = detail?.releaseYear ?: cached?.releaseYear
    val genres: List<String> get() = detail?.genres ?: cached?.genres.orEmpty()

    /** Only block the screen when there is genuinely nothing to show yet. */
    val showLoader: Boolean get() = isLoading && cached == null && detail == null

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
}
