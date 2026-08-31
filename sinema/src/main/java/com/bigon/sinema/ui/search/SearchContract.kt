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
) {
    val canLoadMore: Boolean
        get() = !isSearching && !isAppending && page < totalPages && results.isNotEmpty()

    /**
     * Identifies the list on screen: every input that decides which films these
     * are, and nothing that does not. The view model keeps one set of results
     * per key and the screen keeps one scroll position per key, which is what
     * lets a chip you return to still be where you left it.
     *
     * Those two caches are only useful if they agree on what "the same list"
     * means, so the key is built here, once, and both call this. Appended pages
     * deliberately do not change it — page 2 is the same list, further down.
     */
    val selectionKey: String
        get() = selectionKey(query, selectedGenreId, selectedServiceId, filters)

    companion object {
        fun selectionKey(
            query: String,
            genreId: Int?,
            serviceId: Int?,
            filters: DiscoverFilters,
        ): String = "${query.trim()}|$genreId|$serviceId|$filters"
    }

    val selectedGenreName: String? get() = genres.firstOrNull { it.id == selectedGenreId }?.name

    /**
     * Only meaningful while browsing — see the class comment. Hidden rather
     * than disabled during a typed search: a greyed-out control invites the
     * question "why", and there is no good answer at that moment.
     */
    val showServiceFilter: Boolean get() = query.isBlank() && services.isNotEmpty()

    /**
     * Filters are discover-only because `/search/movie` accepts no `sort_by`,
     * `vote_average.gte` or `with_runtime.lte` — offering them beside a typed
     * query would be a control that does nothing.
     *
     * One exception, recorded rather than acted on: search *does* accept
     * `primary_release_year`, verified against both the reference and the live
     * API (505 results for "spider", 12 once a year is added). So the release
     * year row could work here; it is hidden only because the whole sheet is.
     * An earlier version of this comment claimed the endpoint rejected year
     * too, which was never true.
     */
    val showFilters: Boolean get() = query.isBlank()

    /**
     * Genre is applied client-side once a query is typed, because
     * `/search/movie` does not accept `with_genres` — it takes the parameter
     * without complaint and returns the identical result set, so this is the
     * only way to honour the chip. `/discover/movie` does support it, which is
     * why browse mode filters server-side instead.
     *
     * The cost is real and worth knowing before "optimising" it: each page
     * arrives in full and most of it is discarded. Measured on "spider" +
     * Drama, four requests yielded 19 films from 80 fetched, and the true match
     * count is unknowable without walking all 26 pages.
     */
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
