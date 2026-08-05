package com.bigon.sinema.ui.home

import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.TrendingItem
import com.bigon.core.ui.UiText

/**
 * The screen's three contracts, declared beside its ViewModel: state flows down,
 * intents flow up, effects fire once.
 */
/**
 * What Home is showing. The mixed feed is not a [MovieCategory] because it is
 * not a list of movies — it carries series and people too, has no per-category
 * pagination, and lives in its own table. Folding it into that enum would force
 * every `when` over categories to handle a case that has no movie list.
 */
sealed interface HomeFeed {
    data class Category(val category: MovieCategory) : HomeFeed
    data object AcrossTypes : HomeFeed

    val label: String
        get() = when (this) {
            is Category -> category.label
            AcrossTypes -> "Across Types"
        }
}

data class HomeUiState(
    val feed: HomeFeed = HomeFeed.Category(MovieCategory.Default),
    val movies: List<Movie> = emptyList(),
    val trendingItems: List<TrendingItem> = emptyList(),
    val isRefreshing: Boolean = false,
    /** A next page is being appended below the existing content. */
    val isAppending: Boolean = false,
    /** TMDB has no further pages for this category; stop asking until refresh. */
    val endReached: Boolean = false,
    /** Set when a refresh failed. Cached content, if any, stays on screen. */
    val error: UiText? = null,
) {
    val isAcrossTypes: Boolean get() = feed is HomeFeed.AcrossTypes

    val category: MovieCategory?
        get() = (feed as? HomeFeed.Category)?.category

    /** Whatever the current feed has, counted uniformly. */
    private val itemCount: Int get() = if (isAcrossTypes) trendingItems.size else movies.size

    /**
     * The mixed feed is a single ranked page from TMDB, not a paginated list —
     * there is no "next page" of a weekly ranking to ask for.
     */
    val canLoadMore: Boolean
        get() = !isAcrossTypes && !isAppending && !isRefreshing && !endReached && movies.isNotEmpty()

    /** Skeletons belong on a first load only — never over content we already have. */
    val showSkeletons: Boolean get() = isRefreshing && itemCount == 0
    val showEmptyState: Boolean get() = !isRefreshing && itemCount == 0 && error == null
}

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object LoadMore : HomeIntent
    data class FeedSelected(val feed: HomeFeed) : HomeIntent
    data class TrendingItemClicked(val item: TrendingItem) : HomeIntent
    data class MovieClicked(val movie: Movie) : HomeIntent
    data object ErrorDismissed : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val movieId: Long) : HomeEffect

    /**
     * Series and people have no native screen yet (Tier 3), so they open
     * TMDB's own page. A card that does nothing when tapped teaches the user
     * the feed is broken; one that leaves the app is at least truthful.
     */
    data class OpenExternal(val url: String) : HomeEffect
}
