package com.bigon.data.movie

import com.bigon.core.common.AppResult
import com.bigon.core.common.DispatcherProvider
import com.bigon.core.common.map
import com.bigon.core.database.GenreDao
import com.bigon.core.database.MovieDao
import com.bigon.core.database.MovieDetailDao
import com.bigon.core.database.MovieDetailEntity
import com.bigon.core.database.TrendingItemDao
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
import com.bigon.core.model.Region
import com.bigon.core.model.ReviewPage
import com.bigon.core.model.TrendingItem
import com.bigon.core.model.WatchProvider
import com.bigon.core.datastore.PreferenceStorage
import com.bigon.core.network.ApiCaller
import com.bigon.domain.movie.LoadMoreOutcome
import com.bigon.domain.movie.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first: the database is the single source of truth. Reads never touch
 * the network, and a failed refresh leaves the cached list untouched — the user
 * keeps seeing content, and the caller decides how to surface the failure.
 *
 * Genre names are joined in Kotlin rather than SQL: ids live on the movie row
 * and names in their own table, so both flows are combined and resolved here.
 */
@Singleton
class DefaultMovieRepository @Inject constructor(
    private val movieDao: MovieDao,
    private val genreDao: GenreDao,
    private val movieDetailDao: MovieDetailDao,
    private val trendingItemDao: TrendingItemDao,
    private val movieApi: MovieApi,
    private val apiCaller: ApiCaller,
    private val dispatchers: DispatcherProvider,
    private val regionProvider: RegionProvider,
    private val preferences: PreferenceStorage,
) : MovieRepository {

    override fun observeCategory(category: MovieCategory): Flow<List<Movie>> =
        combine(
            movieDao.observeByList(category.listKey),
            genreDao.observeAll(),
        ) { rows, genres ->
            val genresById = genres.associate { it.id to it.name }
            rows.map { MovieMapper.toDomain(it, genresById) }
        }.flowOn(dispatchers.io)

    override fun observeCached(movieId: Long): Flow<Movie?> =
        combine(
            movieDao.observeById(movieId),
            genreDao.observeAll(),
        ) { row, genres ->
            row?.let { MovieMapper.toDomain(it, genres.associate { g -> g.id to g.name }) }
        }.flowOn(dispatchers.io)

    /**
     * Offline-first, like every other read: the network is tried first because
     * recommendations and streaming availability go stale, but a failure falls
     * back to the cached payload rather than surfacing an error over a title
     * the user has already seen.
     */
    override suspend fun detail(movieId: Long): AppResult<MovieDetail> =
        withContext(dispatchers.io) {
            val region = regionProvider.region()
            val genresById = genreDao.getAll().associate { it.id to it.name }

            when (val result = apiCaller.execute { movieApi.detail(movieId) }) {
                is AppResult.Success -> {
                    val detail = MovieMapper.toDetail(
                        result.value,
                        genresById,
                        region,
                        regionProvider.language(),
                    )
                    cacheDetail(detail, region)
                    AppResult.Success(detail)
                }

                is AppResult.Failure -> cachedDetail(movieId) ?: result
            }
        }

    override fun observeTrendingAll(): Flow<List<TrendingItem>> =
        combine(
            trendingItemDao.observeAll(),
            genreDao.observeAll(),
        ) { rows, genres ->
            val genresById = genres.associate { it.id to it.name }
            // mapNotNull, because an unknown media type maps to null rather
            // than to a card nobody can interpret.
            rows.mapNotNull { MovieMapper.toTrendingItem(it, genresById) }
        }.flowOn(dispatchers.io)

    override suspend fun refreshTrendingAll(): AppResult<Unit> =
        withContext(dispatchers.io) {
            refreshGenresIfNeeded()
            apiCaller.execute { movieApi.trendingAll(page = 1) }
                .map { response -> trendingItemDao.replaceAll(MovieMapper.toTrendingEntities(response)) }
        }

    override suspend fun availableRegions(): AppResult<List<Region>> =
        withContext(dispatchers.io) {
            apiCaller.execute { movieApi.regions() }
                .map { response ->
                    response.results
                        .filter { it.code.length == 2 && it.englishName.isNotBlank() }
                        .map { Region(code = it.code.uppercase(), name = it.englishName) }
                        .sortedBy { it.name }
                }
        }

    override suspend fun activeRegion(): String = regionProvider.region()

    override suspend fun setRegion(code: String?) = withContext(dispatchers.io) {
        if (code == regionProvider.region() && code != null) return@withContext
        preferences.setRegion(code)
        // Certification, availability and the curated lists were all fetched
        // for the previous region. Detail payloads carry it baked in, so they
        // go too — trending is left alone, being region-independent.
        movieDao.clearAll()
        movieDetailDao.clear()
    }

    override suspend fun reviews(movieId: Long, page: Int): AppResult<ReviewPage> =
        withContext(dispatchers.io) {
            apiCaller.execute { movieApi.reviews(movieId, page) }.map(MovieMapper::toReviewPage)
        }

    private suspend fun cacheDetail(detail: MovieDetail, region: String) {
        runCatching {
            movieDetailDao.save(
                MovieDetailEntity(
                    id = detail.id,
                    region = region,
                    payload = detailJson.encodeToString(detail.toSnapshot()),
                    fetchedAt = System.currentTimeMillis(),
                ),
                keep = MAX_CACHED_DETAILS,
            )
        }
        // A cache write failing must not fail the read it was serving.
    }

    private suspend fun cachedDetail(movieId: Long): AppResult<MovieDetail>? {
        val row = movieDetailDao.byId(movieId) ?: return null
        return runCatching {
            AppResult.Success(detailJson.decodeFromString<MovieDetailSnapshot>(row.payload).toDomain())
        }.getOrNull()
        // A payload written by an older snapshot shape decodes to null and is
        // treated as a miss, so a format change degrades to a network read.
    }

    /** total_pages per list, learned from responses. In-memory only: after
     *  process death the next loadMore simply asks TMDB and finds out. */
    private val totalPagesByList = mutableMapOf<String, Int>()

    private companion object {
        /**
         * Detail payloads are roughly an order of magnitude larger than list
         * rows, so the cache is bounded rather than left to grow silently.
         * Fifty is well past any plausible back-and-forth browsing session and
         * still well under a megabyte.
         */
        const val MAX_CACHED_DETAILS = 50

        /**
         * `ignoreUnknownKeys` is what lets the snapshot format gain fields
         * without invalidating everything already on disk.
         */
        val detailJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }

    override suspend fun refresh(category: MovieCategory): AppResult<Unit> =
        withContext(dispatchers.io) {
            // Genre names are a prerequisite for rendering, but a stale or empty
            // genre table must not fail the whole refresh.
            refreshGenresIfNeeded()

            apiCaller.execute { movieApi.fetch(category, page = 1, region = regionProvider.region()) }
                .map { response ->
                    totalPagesByList[category.listKey] = response.totalPages
                    val entities = response.results.mapIndexed { index, dto ->
                        MovieMapper.toEntity(dto, category.listKey, page = 1, position = index)
                    }
                    movieDao.replaceList(category.listKey, entities)
                }
        }

    override suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome> =
        withContext(dispatchers.io) {
            val listKey = category.listKey
            val lastPage = movieDao.maxPage(listKey)
                // Nothing cached yet: loading "more" of nothing is a refresh.
                ?: return@withContext refresh(category).map { LoadMoreOutcome.Appended }

            totalPagesByList[listKey]?.let { total ->
                if (lastPage >= total) return@withContext AppResult.Success(LoadMoreOutcome.EndReached)
            }

            apiCaller.execute { movieApi.fetch(category, page = lastPage + 1, region = regionProvider.region()) }
                .map { response ->
                    totalPagesByList[listKey] = response.totalPages
                    if (response.results.isEmpty()) {
                        LoadMoreOutcome.EndReached
                    } else {
                        // Positions continue after the cached rows so ordering
                        // survives duplicates being dropped by the DAO.
                        val base = (movieDao.maxPosition(listKey) ?: -1) + 1
                        val entities = response.results.mapIndexed { index, dto ->
                            MovieMapper.toEntity(dto, listKey, page = lastPage + 1, position = base + index)
                        }
                        movieDao.appendList(entities)
                        LoadMoreOutcome.Appended
                    }
                }
        }

    override fun observeGenres(): Flow<List<Genre>> =
        genreDao.observeAll()
            .map { rows -> rows.map { Genre(id = it.id, name = it.name) } }
            .flowOn(dispatchers.io)

    override suspend fun search(query: String, page: Int): AppResult<MoviePage> =
        withContext(dispatchers.io) {
            refreshGenresIfNeeded()
            val genresById = genreDao.getAll().associate { it.id to it.name }
            apiCaller.execute { movieApi.search(query, page = page) }
                .map { response -> MovieMapper.toPage(response, genresById) }
        }

    override suspend fun discover(
        genreId: Int?,
        page: Int,
        streamingProviderId: Int?,
    ): AppResult<MoviePage> =
        withContext(dispatchers.io) {
            refreshGenresIfNeeded()
            val genresById = genreDao.getAll().associate { it.id to it.name }
            apiCaller.execute {
                movieApi.discover(
                    withGenres = genreId?.toString(),
                    page = page,
                    withWatchProviders = streamingProviderId?.toString(),
                    // Only sent alongside a provider; on its own it would
                    // narrow results for no reason the user asked for.
                    watchRegion = streamingProviderId?.let { regionProvider.region() },
                )
            }.map { response -> MovieMapper.toPage(response, genresById) }
        }

    override suspend fun streamingServices(): AppResult<List<WatchProvider>> =
        withContext(dispatchers.io) {
            apiCaller.execute { movieApi.watchProviders(regionProvider.region()) }
                .map { response ->
                    response.results
                        .sortedBy { it.displayPriority }
                        .map {
                            WatchProvider(
                                id = it.providerId,
                                name = it.providerName,
                                logoUrl = TmdbImageUrl.build(it.logoPath, TmdbImageUrl.LOGO_SMALL),
                            )
                        }
                }
        }

    override suspend fun clearCatalogCache() = withContext(dispatchers.io) {
        movieDao.clearAll()
        movieDetailDao.clear()
        trendingItemDao.clear()
        // Genres are cheap to refetch and refetched lazily on next use.
    }

    private suspend fun refreshGenresIfNeeded() {
        if (genreDao.count() > 0) return
        apiCaller.execute { movieApi.genres() }
            .map { response -> genreDao.upsertAll(response.genres.map(MovieMapper::toGenreEntity)) }
    }
}

/** Stable cache key for a category — deliberately not the enum's ordinal. */
internal val MovieCategory.listKey: String
    get() = when (this) {
        MovieCategory.Trending -> "trending"
        MovieCategory.TrendingWeek -> "trending_week"
        MovieCategory.Popular -> "popular"
        MovieCategory.NowPlaying -> "now_playing"
        MovieCategory.TopRated -> "top_rated"
        MovieCategory.Upcoming -> "upcoming"
    }

/**
 * [region] localises the four curated lists. Trending is deliberately not
 * given one — TMDB does not accept it there, and a global trend is the point.
 */
private suspend fun MovieApi.fetch(
    category: MovieCategory,
    page: Int,
    region: String?,
): MovieListResponse = when (category) {
    MovieCategory.Trending -> trending(page)
    MovieCategory.TrendingWeek -> trendingWeek(page)
    MovieCategory.Popular -> popular(page, region)
    MovieCategory.NowPlaying -> nowPlaying(page, region)
    MovieCategory.TopRated -> topRated(page, region)
    MovieCategory.Upcoming -> upcoming(page, region)
}
