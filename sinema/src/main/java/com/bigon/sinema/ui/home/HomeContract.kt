package com.bigon.sinema.ui.home

import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.ui.UiText

/**
 * The screen's three contracts, declared beside its ViewModel: state flows down,
 * intents flow up, effects fire once.
 */
data class HomeUiState(
    val category: MovieCategory = MovieCategory.Default,
    val movies: List<Movie> = emptyList(),
    val isRefreshing: Boolean = false,
    /** A next page is being appended below the existing content. */
    val isAppending: Boolean = false,
    /** TMDB has no further pages for this category; stop asking until refresh. */
    val endReached: Boolean = false,
    /** Set when a refresh failed. Cached content, if any, stays on screen. */
    val error: UiText? = null,
) {
    val canLoadMore: Boolean get() = !isAppending && !isRefreshing && !endReached && movies.isNotEmpty()

    /** Skeletons belong on a first load only — never over content we already have. */
    val showSkeletons: Boolean get() = isRefreshing && movies.isEmpty()
    val showEmptyState: Boolean get() = !isRefreshing && movies.isEmpty() && error == null
}

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object LoadMore : HomeIntent
    data class CategorySelected(val category: MovieCategory) : HomeIntent
    data class MovieClicked(val movie: Movie) : HomeIntent
    data object ErrorDismissed : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val movieId: Long) : HomeEffect
}
