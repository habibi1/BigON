package com.bigon.sinema.ui.search

import com.bigon.tmdb.model.Genre
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.WatchProvider
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.core.ui.UiText

/**
 * Hybrid semantics, chosen because TMDB's search endpoint takes no genre filter:
 *  - blank query  → `/discover/movie` with the selected genre (server-side) —
 *    the tab has browsable content before a single keystroke;
 *  - typed query  → `/search/movie`, with the selected genre applied as a
 *    client-side filter over the returned page.
 *
 * The streaming filter follows the same asymmetry for the same reason, but
 * cannot be papered over client-side: `/search/movie` accepts no provider
 * filter, and search results carry no provider data to filter on. So the
 * service row appears only while browsing, rather than sitting inert beside a
 * typed query pretending to work.
 */
data class SearchUiState(
    val query: String = "",
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val services: List<WatchProvider> = emptyList(),
    val selectedServiceId: Int? = null,
    val filters: DiscoverFilters = DiscoverFilters(),
    val isFilterSheetOpen: Boolean = false,
    val results: List<Movie> = emptyList(),
    val isSearching: Boolean = true,
    /** A further page is being appended below the current results. */
    val isAppending: Boolean = false,
    val page: Int = 1,
    val totalPages: Int = 1,
    val error: UiText? = null,
    /**
     * Bumped when the results are replaced wholesale — a new query, genre,
     * service or filter. The screen scrolls to the top on a change. Appended
     * pages deliberately do not bump it.
     */
    val contentGeneration: Int = 0,
) {
    val canLoadMore: Boolean
        get() = !isSearching && !isAppending && page < totalPages && results.isNotEmpty()

    val selectedGenreName: String? get() = genres.firstOrNull { it.id == selectedGenreId }?.name

    /**
     * Only meaningful while browsing — see the class comment. Hidden rather
     * than disabled during a typed search: a greyed-out control invites the
     * question "why", and there is no good answer at that moment.
     */
    val showServiceFilter: Boolean get() = query.isBlank() && services.isNotEmpty()

    /**
     * Refinements are discover-only for the same reason the service row is:
     * `/search/movie` accepts no sort, year, rating or runtime parameter, so
     * offering them beside a typed query would be a control that does nothing.
     */
    val showFilters: Boolean get() = query.isBlank()

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
    data class ServiceSelected(val serviceId: Int?) : SearchIntent
    data object FilterSheetOpened : SearchIntent
    data object FilterSheetDismissed : SearchIntent
    data class FiltersChanged(val filters: DiscoverFilters) : SearchIntent
    data class MovieClicked(val movie: Movie) : SearchIntent
    data object Retry : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val movieId: Long) : SearchEffect
}
