package com.bigon.tmdb.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.tmdb.model.Genre
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieCategory
import com.bigon.tmdb.model.MovieDetail
import com.bigon.tmdb.model.MoviePage
import com.bigon.tmdb.model.MovieCollection
import com.bigon.tmdb.model.PersonDetail
import com.bigon.tmdb.model.Region
import com.bigon.tmdb.model.TvDetail
import com.bigon.tmdb.model.ReviewPage
import com.bigon.tmdb.model.TrendingItem
import com.bigon.tmdb.model.WatchProvider
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

    /**
     * Genre- and provider-filtered browsing; null on either means unfiltered.
     *
     * [streamingProviderId] is resolved against the caller's region inside the
     * data layer, because TMDB ignores a provider filter with no region and
     * does so silently — the filter appears to work and simply does nothing.
     */
    suspend fun discover(
        genreId: Int?,
        page: Int = 1,
        streamingProviderId: Int? = null,
        filters: DiscoverFilters = DiscoverFilters(),
    ): AppResult<MoviePage>

    /**
     * Services carrying films in the user's region, ordered by TMDB's display
     * priority. Region-dependent by nature: 289 in the US, 46 in Indonesia.
     */
    suspend fun streamingServices(): AppResult<List<WatchProvider>>

    /** Wipes cached catalogue lists and genres. Favourites are untouched. */
    /**
     * Paginated separately from detail. Never cached: reviews are read once and
     * churn, so staleness would cost more than the storage saves.
     */
    suspend fun reviews(movieId: Long, page: Int = 1): AppResult<ReviewPage>

    /**
     * The mixed trending feed — films, series and people in one ranking.
     * Cached like every other list, so it survives going offline.
     */
    fun observeTrendingAll(): Flow<List<TrendingItem>>

    suspend fun refreshTrendingAll(): AppResult<Unit>

    /** A franchise and its parts, ordered by release. Network-only. */
    suspend fun collection(collectionId: Long): AppResult<MovieCollection>

    /** A person and their merged cast/crew filmography. Network-only. */
    suspend fun person(personId: Long): AppResult<PersonDetail>

    /** Series detail, region-scoped for certification and availability. */
    suspend fun tvDetail(tvId: Long): AppResult<TvDetail>

    /** Regions the user may choose between, alphabetical by name. */
    suspend fun availableRegions(): AppResult<List<Region>>

    /**
     * The region actually in use, whether chosen or inherited from the device.
     * Needed so Settings can show what is in effect rather than a blank row.
     */
    suspend fun activeRegion(): String

    /**
     * Persists a region, or null to follow the device, and drops the caches
     * that were built for the old one.
     *
     * Certification, streaming availability and the curated lists are all
     * region-scoped, so keeping them would leave the app showing one region's
     * answers under another region's label.
     */
    suspend fun setRegion(code: String?)

    suspend fun clearCatalogCache()
}

enum class LoadMoreOutcome { Appended, EndReached }
