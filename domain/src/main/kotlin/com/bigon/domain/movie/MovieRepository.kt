package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
import kotlinx.coroutines.flow.Flow

/**
 * The domain's view of movie storage. Implemented in :data, injected everywhere
 * else — so the rest of the app never learns that TMDB or Room exist.
 *
 * Offline-first contract: [observeCategory] is backed by the local database and
 * emits whatever is cached, immediately and without a network call. [refresh]
 * updates that cache and reports success or failure separately, so a failed
 * refresh never blanks content the user can still read.
 */
interface MovieRepository {

    /** Cached titles for a category. Main-safe; never throws. */
    fun observeCategory(category: MovieCategory): Flow<List<Movie>>

    /** Fetches page 1 from the network and replaces the cached category. */
    suspend fun refresh(category: MovieCategory): AppResult<Unit>

    /**
     * Fetches the next page of a category and appends it to the cache.
     * [LoadMoreOutcome.EndReached] means there is nothing further to ask for;
     * callers should stop triggering until the next [refresh].
     */
    suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome>

    /**
     * The cached list projection of one title, if any category has it. Lets the
     * detail screen paint real content instantly instead of a spinner.
     */
    fun observeCached(movieId: Long): Flow<Movie?>

    /** Full detail. Network-only today — not yet persisted for offline reads. */
    suspend fun detail(movieId: Long): AppResult<MovieDetail>

    /** The cached genre table — drives the Search tab's chips. */
    fun observeGenres(): Flow<List<Genre>>

    /** Free-text search. Network-only; results are never cached. */
    suspend fun search(query: String, page: Int = 1): AppResult<MoviePage>

    /** Genre-filtered browsing ([genreId] null = most popular overall). */
    suspend fun discover(genreId: Int?, page: Int = 1): AppResult<MoviePage>

    /** Wipes cached catalogue lists and genres. Favourites are untouched. */
    suspend fun clearCatalogCache()
}

enum class LoadMoreOutcome { Appended, EndReached }
