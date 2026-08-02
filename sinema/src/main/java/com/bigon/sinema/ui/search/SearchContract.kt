package com.bigon.sinema.ui.search

import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.ui.UiText

/**
 * Hybrid semantics, chosen because TMDB's search endpoint takes no genre filter:
 *  - blank query  → `/discover/movie` with the selected genre (server-side) —
 *    the tab has browsable content before a single keystroke;
 *  - typed query  → `/search/movie`, with the selected genre applied as a
 *    client-side filter over the returned page.
 */
data class SearchUiState(
    val query: String = "",
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val results: List<Movie> = emptyList(),
    val isSearching: Boolean = true,
    /** A further page is being appended below the current results. */
    val isAppending: Boolean = false,
    val page: Int = 1,
    val totalPages: Int = 1,
    val error: UiText? = null,
) {
    val canLoadMore: Boolean
        get() = !isSearching && !isAppending && page < totalPages && results.isNotEmpty()

    val selectedGenreName: String? get() = genres.firstOrNull { it.id == selectedGenreId }?.name

    /** Genre filter applied client-side when a typed search is active. */
    val visibleResults: List<Movie>
        get() = if (query.isBlank() || selectedGenreName == null) {
            results
        } else {
            results.filter { selectedGenreName in it.genres }
        }

    val showSkeletons: Boolean get() = isSearching && visibleResults.isEmpty() && error == null
    val showEmptyState: Boolean get() = !isSearching && visibleResults.isEmpty() && error == null
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object LoadMore : SearchIntent
    data class GenreSelected(val genreId: Int?) : SearchIntent
    data class MovieClicked(val movie: Movie) : SearchIntent
    data object Retry : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val movieId: Long) : SearchEffect
}
