package com.bigon.tmdb.data.movie

import app.cash.turbine.test
import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.common.DispatcherProvider
import com.bigon.tmdb.database.FavoriteDao
import com.bigon.tmdb.database.FavoriteEntity
import com.bigon.tmdb.database.GenreDao
import com.bigon.tmdb.database.GenreEntity
import com.bigon.tmdb.database.MovieDao
import com.bigon.tmdb.database.MovieDetailDao
import com.bigon.tmdb.database.MovieDetailEntity
import com.bigon.tmdb.database.TrendingItemDao
import com.bigon.tmdb.database.TrendingItemEntity
import com.bigon.core.datastore.PreferenceStorage
import com.bigon.tmdb.database.MovieEntity
import com.bigon.tmdb.model.MovieCategory
import com.bigon.core.network.ApiCaller
import com.bigon.core.network.error.NetworkErrorMapper
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.tmdb.domain.movie.DiscoverSort
import com.bigon.tmdb.domain.movie.LoadMoreOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The offline-first contract: reads come from the database, and a failed refresh
 * must never destroy content the user can still read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMovieRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeMovieDao : MovieDao {
        val rows = MutableStateFlow<Map<String, List<MovieEntity>>>(emptyMap())
        override fun observeByList(listKey: String): Flow<List<MovieEntity>> =
            rows.map { it[listKey].orEmpty() }
        override fun observeById(id: Long): Flow<MovieEntity?> = MutableStateFlow(null)
        override suspend fun insertIgnoringDuplicates(movies: List<MovieEntity>) {
            val key = movies.firstOrNull()?.listKey ?: return
            val existing = rows.value[key].orEmpty()
            val existingIds = existing.map { it.id }.toSet()
            rows.value = rows.value + (key to (existing + movies.filterNot { it.id in existingIds }))
        }
        override suspend fun maxPage(listKey: String): Int? =
            rows.value[listKey]?.maxOfOrNull { it.page }
        override suspend fun maxPosition(listKey: String): Int? =
            rows.value[listKey]?.maxOfOrNull { it.position }
        override suspend fun deleteList(listKey: String) {
            rows.value = rows.value + (listKey to emptyList())
        }
        override suspend fun clearAll() {
            rows.value = emptyMap()
        }
    }

    /** In-memory stand-in for the favourites table. */
    class FakeFavoriteDao : FavoriteDao {
        val rows = MutableStateFlow<Map<Long, FavoriteEntity>>(emptyMap())
        override fun observeAll(): Flow<List<FavoriteEntity>> = rows.map { it.values.toList() }
        override fun observeIsFavorite(movieId: Long): Flow<Boolean> = rows.map { movieId in it }
        override suspend fun byId(movieId: Long): FavoriteEntity? = rows.value[movieId]
        override suspend fun upsert(favorite: FavoriteEntity) {
            rows.value = rows.value + (favorite.id to favorite)
        }
        override suspend fun delete(movieId: Long) {
            rows.value = rows.value - movieId
        }
    }

    private class FakeGenreDao(initial: List<GenreEntity> = emptyList()) : GenreDao {
        val genres = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<GenreEntity>> = genres
        override suspend fun getAll(): List<GenreEntity> = genres.value
        override suspend fun count(): Int = genres.value.size
        override suspend fun upsertAll(genres: List<GenreEntity>) {
            this.genres.value = this.genres.value + genres
        }
    }

    private class FakeApi(
        private val onList: () -> MovieListResponse,
        private val onGenres: () -> GenreListResponse = { GenreListResponse() },
        private val onDetail: (Long) -> MovieDetailResponse = { MovieDetailResponse(id = it) },
    ) : MovieApi {
        val requestedPages = mutableListOf<Int>()
        override suspend fun trending(page: Int, language: String?) = onList().also { requestedPages += page }
        override suspend fun trendingWeek(page: Int, language: String?) = onList().also { requestedPages += page }
        override suspend fun popular(page: Int, region: String?, language: String?) = onList().also { requestedPages += page }
        override suspend fun nowPlaying(page: Int, region: String?, language: String?) = onList().also { requestedPages += page }
        override suspend fun topRated(page: Int, region: String?, language: String?) = onList().also { requestedPages += page }
        override suspend fun upcoming(page: Int, region: String?, language: String?) = onList().also { requestedPages += page }
        override suspend fun genres(language: String?) = onGenres()
        var detailCalls = 0
        override suspend fun detail(id: Long, append: String, imageLanguage: String?, language: String?) =
            onDetail(id).also { detailCalls++ }
        override suspend fun reviews(id: Long, page: Int, language: String?) = ReviewListResponse()
        var lastSearchQuery: String? = null
        var lastDiscoverGenres: String? = null
        var lastRequestedPage: Int? = null
        override suspend fun search(
            query: String,
            page: Int,
            includeAdult: Boolean,
            primaryReleaseYear: Int?,
            year: Int?,
            region: String?,
            language: String?,
        ): MovieListResponse {
            lastSearchQuery = query; lastRequestedPage = page; return onList()
        }
        var lastWatchProviders: String? = null
        var lastWatchRegion: String? = null
        var lastSortBy: String? = null
        var lastReleaseYear: Int? = null
        var lastMinRating: Double? = null
        var lastMinVotes: Int? = null
        var lastMaxRuntime: Int? = null
        @Suppress("LongParameterList")
        override suspend fun discover(
            withGenres: String?,
            sortBy: String,
            page: Int,
            withWatchProviders: String?,
            watchRegion: String?,
            primaryReleaseYear: Int?,
            minRating: Double?,
            minVotes: Int?,
            maxRuntime: Int?,
            certification: String?,
            certificationGte: String?,
            certificationLte: String?,
            certificationCountry: String?,
            year: Int?,
            primaryReleaseDateGte: String?,
            primaryReleaseDateLte: String?,
            releaseDateGte: String?,
            releaseDateLte: String?,
            withReleaseType: String?,
            maxRating: Double?,
            maxVotes: Int?,
            minRuntime: Int?,
            withCast: String?,
            withCrew: String?,
            withPeople: String?,
            withCompanies: String?,
            withoutCompanies: String?,
            withKeywords: String?,
            withoutKeywords: String?,
            withOriginCountry: String?,
            withOriginalLanguage: String?,
            withoutGenres: String?,
            withWatchMonetizationTypes: String?,
            withoutWatchProviders: String?,
            includeAdult: Boolean?,
            includeVideo: Boolean?,
            region: String?,
            language: String?,
        ): MovieListResponse {
            lastDiscoverGenres = withGenres; lastRequestedPage = page
            lastWatchProviders = withWatchProviders; lastWatchRegion = watchRegion
            lastSortBy = sortBy; lastReleaseYear = primaryReleaseYear
            lastMinRating = minRating; lastMinVotes = minVotes; lastMaxRuntime = maxRuntime
            return onList()
        }
        override suspend fun watchProviders(watchRegion: String, language: String?) = WatchProviderListResponse()
        override suspend fun trendingAll(page: Int, language: String?) = TrendingListResponse()
        override suspend fun regions(language: String?) = RegionListResponse()
        override suspend fun collection(id: Long, language: String?) = CollectionResponse()
        override suspend fun person(id: Long, append: String, language: String?) = PersonResponse()
        override suspend fun tvDetail(id: Long, append: String, language: String?) = TvDetailResponse()
    }

    private fun dispatchers() = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = StandardTestDispatcher()
    }



    /** In-memory PreferenceStorage; only the region key matters to these tests. */
    class FakePreferences(initialRegion: String? = null) : PreferenceStorage {
        val regionFlow = MutableStateFlow(initialRegion)
        override val region: Flow<String?> = regionFlow
        override suspend fun setRegion(value: String?) { regionFlow.value = value }
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setOnboardingCompleted(value: Boolean) = Unit
        override val themeMode: Flow<String?> = MutableStateFlow(null)
        override suspend fun setThemeMode(value: String) = Unit
    }

    /** In-memory stand-in for the mixed trending cache. */
    class FakeTrendingItemDao : TrendingItemDao {
        val rows = MutableStateFlow<List<TrendingItemEntity>>(emptyList())
        override fun observeAll(): Flow<List<TrendingItemEntity>> = rows
        override suspend fun insertAll(items: List<TrendingItemEntity>) {
            rows.value = rows.value + items
        }
        override suspend fun clear() {
            rows.value = emptyList()
        }
    }

    /** In-memory stand-in for the detail cache; the real DAO is Room-generated. */
    class FakeMovieDetailDao : MovieDetailDao {
        val rows = mutableMapOf<Long, MovieDetailEntity>()
        var failWrites = false

        override suspend fun byId(movieId: Long): MovieDetailEntity? = rows[movieId]

        override suspend fun upsert(entity: MovieDetailEntity) {
            if (failWrites) error("disk full")
            rows[entity.id] = entity
        }

        override suspend fun clear() = rows.clear()
        override suspend fun count(): Int = rows.size

        override suspend fun trimTo(keep: Int) {
            rows.entries.sortedByDescending { it.value.fetchedAt }
                .drop(keep)
                .forEach { rows.remove(it.key) }
        }
    }

    private fun repository(
        movieDao: MovieDao,
        genreDao: GenreDao,
        api: MovieApi,
        detailDao: MovieDetailDao = FakeMovieDetailDao(),
        trendingDao: TrendingItemDao = FakeTrendingItemDao(),
        region: String = "US",
        preferences: PreferenceStorage = FakePreferences(),
        favoriteDao: FavoriteDao = FakeFavoriteDao(),
    ) = DefaultMovieRepository(
        movieDao,
        genreDao,
        favoriteDao,
        detailDao,
        trendingDao,
        api,
        ApiCaller(NetworkErrorMapper()),
        dispatchers(),
        object : RegionProvider {
            override suspend fun region() = region
            override fun language() = "en"
        },
        preferences,
    )

    private fun movieDto(id: Long, title: String) =
        MovieDto(id = id, title = title, voteAverage = 8.0, voteCount = 10, genreIds = listOf(28))

    @Test
    fun `refresh stores the network list and reading returns it mapped`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao(listOf(GenreEntity(28, "Action")))
        val repo = repository(
            movieDao,
            genreDao,
            FakeApi(onList = { MovieListResponse(results = listOf(movieDto(1, "Heat"))) }),
        )

        assertIs<AppResult.Success<Unit>>(repo.refresh(MovieCategory.Popular))

        repo.observeCategory(MovieCategory.Popular).test {
            val movies = awaitItem()
            assertEquals(listOf("Heat"), movies.map { it.title })
            assertEquals(listOf("Action"), movies.first().genres)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed refresh reports the error and leaves cached content intact`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao(listOf(GenreEntity(28, "Action")))

        // First refresh succeeds and populates the cache.
        var failing = false
        val repo = repository(
            movieDao,
            genreDao,
            FakeApi(onList = {
                if (failing) throw IOException("offline") else MovieListResponse(results = listOf(movieDto(1, "Heat")))
            }),
        )
        repo.refresh(MovieCategory.Popular)

        // Second refresh fails.
        failing = true
        val result = repo.refresh(MovieCategory.Popular)

        assertIs<AppResult.Failure>(result)
        assertIs<AppError.Network.NoConnection>(result.error)

        repo.observeCategory(MovieCategory.Popular).test {
            assertEquals(listOf("Heat"), awaitItem().map { it.title }) // still readable
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshing one category does not evict another`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao()
        var title = "Popular One"
        val repo = repository(
            movieDao,
            genreDao,
            FakeApi(onList = { MovieListResponse(results = listOf(movieDto(1, title))) }),
        )

        repo.refresh(MovieCategory.Popular)
        title = "Trending One"
        repo.refresh(MovieCategory.Trending)

        repo.observeCategory(MovieCategory.Popular).test {
            assertEquals(listOf("Popular One"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
        repo.observeCategory(MovieCategory.Trending).test {
            assertEquals(listOf("Trending One"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `genres are fetched once and reused`() = runTest(dispatcher) {
        val genreDao = FakeGenreDao()
        var genreCalls = 0
        val repo = repository(
            FakeMovieDao(),
            genreDao,
            FakeApi(
                onList = { MovieListResponse(results = listOf(movieDto(1, "Heat"))) },
                onGenres = {
                    genreCalls++
                    GenreListResponse(genres = listOf(GenreDto(28, "Action")))
                },
            ),
        )

        repo.refresh(MovieCategory.Popular)
        repo.refresh(MovieCategory.Popular)

        assertEquals(1, genreCalls)
        assertTrue(genreDao.genres.value.isNotEmpty())
    }

    @Test
    fun `search maps results with genre names and never writes the cache`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        val api = FakeApi(onList = { MovieListResponse(results = listOf(movieDto(7, "Dune"))) })
        val repo = repository(movieDao, FakeGenreDao(listOf(GenreEntity(28, "Action"))), api)

        val result = repo.search("dune")

        val page = (result as AppResult.Success).value
        assertEquals(listOf("Dune"), page.movies.map { it.title })
        assertEquals(listOf("Action"), page.movies.first().genres)
        assertEquals("dune", api.lastSearchQuery)
        assertTrue(movieDao.rows.value.isEmpty()) // transient — cache untouched
    }

    @Test
    fun `discover forwards the genre id and null means unfiltered`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse(results = listOf(movieDto(7, "Heat"))) })
        val repo = repository(FakeMovieDao(), FakeGenreDao(listOf(GenreEntity(28, "Action"))), api)

        repo.discover(genreId = 878)
        assertEquals("878", api.lastDiscoverGenres)

        repo.discover(genreId = null)
        assertEquals(null, api.lastDiscoverGenres)
    }

    @Test
    fun `refinements travel as discover query parameters`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse() })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        repo.discover(
            genreId = null,
            filters = DiscoverFilters(
                sort = DiscoverSort.Rating,
                releaseYear = 2024,
                maxRuntimeMinutes = 120,
            ),
        )

        assertEquals("vote_average.desc", api.lastSortBy)
        assertEquals(2024, api.lastReleaseYear)
        assertEquals(120, api.lastMaxRuntime)
    }

    @Test
    fun `a rating floor always travels with a vote-count floor`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse() })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        // Without the vote-count guard, "8.0+" surfaces obscure titles rated
        // 10.0 by three people ahead of everything the user has heard of.
        repo.discover(genreId = null, filters = DiscoverFilters(minRating = 8.0))
        assertEquals(8.0, api.lastMinRating)
        assertEquals(DiscoverFilters.MIN_VOTES_FOR_RATING_FILTER, api.lastMinVotes)

        // And it is never sent alone: on its own it would quietly exclude every
        // newly released film, which has no votes yet.
        repo.discover(genreId = null, filters = DiscoverFilters(releaseYear = 2026))
        assertEquals(null, api.lastMinRating)
        assertEquals(null, api.lastMinVotes)
    }

    @Test
    fun `default filters leave discover unnarrowed`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse() })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        repo.discover(genreId = null)

        assertEquals("popularity.desc", api.lastSortBy)
        assertEquals(null, api.lastReleaseYear)
        assertEquals(null, api.lastMinRating)
        assertEquals(null, api.lastMinVotes)
        assertEquals(null, api.lastMaxRuntime)
    }

    @Test
    fun `observeGenres exposes the cached table as domain genres`() = runTest(dispatcher) {
        val repo = repository(
            FakeMovieDao(),
            FakeGenreDao(listOf(GenreEntity(28, "Action"), GenreEntity(878, "Science Fiction"))),
            FakeApi(onList = { MovieListResponse() }),
        )
        repo.observeGenres().test {
            assertEquals(listOf("Action", "Science Fiction"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pagination ──────────────────────────────────────────────────────────

    @Test
    fun `loadMore appends the next page below the existing rows`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        var page = 0
        val api = FakeApi(onList = {
            page++
            MovieListResponse(
                page = page,
                results = listOf(movieDto(page * 10L, "Page $page")),
                totalPages = 5,
            )
        })
        val repo = repository(movieDao, FakeGenreDao(), api)

        repo.refresh(MovieCategory.Popular)
        val outcome = repo.loadMore(MovieCategory.Popular)

        assertEquals(LoadMoreOutcome.Appended, (outcome as AppResult.Success).value)
        assertEquals(listOf(1, 2), api.requestedPages) // page 1 then page 2
        repo.observeCategory(MovieCategory.Popular).test {
            assertEquals(listOf("Page 1", "Page 2"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a movie repeated on the next page does not duplicate or reorder`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        var call = 0
        val api = FakeApi(onList = {
            call++
            // Page 2 repeats id 1 — TMDB does this near page boundaries.
            val results = if (call == 1) {
                listOf(movieDto(1, "First"), movieDto(2, "Second"))
            } else {
                listOf(movieDto(2, "Second again"), movieDto(3, "Third"))
            }
            MovieListResponse(page = call, results = results, totalPages = 5)
        })
        val repo = repository(movieDao, FakeGenreDao(), api)

        repo.refresh(MovieCategory.Popular)
        repo.loadMore(MovieCategory.Popular)

        repo.observeCategory(MovieCategory.Popular).test {
            // The first sighting wins and keeps its position.
            assertEquals(listOf("First", "Second", "Third"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMore reports EndReached on the last page and stops requesting`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        val api = FakeApi(onList = {
            MovieListResponse(page = 1, results = listOf(movieDto(1, "Only")), totalPages = 1)
        })
        val repo = repository(movieDao, FakeGenreDao(), api)

        repo.refresh(MovieCategory.Popular)
        val outcome = repo.loadMore(MovieCategory.Popular)

        assertEquals(LoadMoreOutcome.EndReached, (outcome as AppResult.Success).value)
        assertEquals(listOf(1), api.requestedPages) // no page-2 request was made
    }

    @Test
    fun `loadMore on an empty cache falls back to a refresh`() = runTest(dispatcher) {
        val api = FakeApi(onList = {
            MovieListResponse(page = 1, results = listOf(movieDto(1, "First")), totalPages = 3)
        })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        val outcome = repo.loadMore(MovieCategory.Popular)

        assertEquals(LoadMoreOutcome.Appended, (outcome as AppResult.Success).value)
        assertEquals(listOf(1), api.requestedPages)
    }

    @Test
    fun `refresh resets the list to page one after paging deeper`() = runTest(dispatcher) {
        val movieDao = FakeMovieDao()
        var call = 0
        val api = FakeApi(onList = {
            call++
            MovieListResponse(page = call, results = listOf(movieDto(call.toLong(), "M$call")), totalPages = 5)
        })
        val repo = repository(movieDao, FakeGenreDao(), api)

        repo.refresh(MovieCategory.Popular)
        repo.loadMore(MovieCategory.Popular)
        assertEquals(2, movieDao.rows.value["popular"]?.size)

        repo.refresh(MovieCategory.Popular)

        assertEquals(1, movieDao.rows.value["popular"]?.size)
    }

    @Test
    fun `search forwards the requested page`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse(page = 2, results = emptyList(), totalPages = 9) })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        val result = repo.search("dune", page = 2)

        assertEquals(2, api.lastRequestedPage)
        assertEquals(9, (result as AppResult.Success).value.totalPages)
        assertTrue(result.value.hasMore)
    }

    // ── Tier 2: regional lists and the streaming filter ─────────────────────

    @Test
    fun `curated lists carry the region, trending deliberately does not`() = runTest(dispatcher) {
        val api = object : FakeMovieApi() {
            var nowPlayingRegion: String? = "unset"
            var trendingCalled = false

            override suspend fun nowPlaying(
                page: Int,
                region: String?,
                language: String?,
            ): MovieListResponse {
                nowPlayingRegion = region
                return MovieListResponse()
            }

            override suspend fun trending(page: Int, language: String?): MovieListResponse {
                trendingCalled = true
                return MovieListResponse()
            }
        }
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api, region = "ID")

        repo.refresh(MovieCategory.NowPlaying)
        assertEquals("ID", api.nowPlayingRegion)

        // Trending is a global signal and TMDB rejects a region there.
        repo.refresh(MovieCategory.Trending)
        assertTrue(api.trendingCalled)
    }

    @Test
    fun `a streaming filter always travels with a region`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse() })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api, region = "ID")

        repo.discover(genreId = null, page = 1, streamingProviderId = 8)

        assertEquals("8", api.lastWatchProviders)
        // Without this TMDB ignores the filter silently rather than erroring.
        assertEquals("ID", api.lastWatchRegion)
    }

    @Test
    fun `no streaming filter means no region narrowing the results`() = runTest(dispatcher) {
        val api = FakeApi(onList = { MovieListResponse() })
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api, region = "ID")

        repo.discover(genreId = 28, page = 1, streamingProviderId = null)

        assertEquals(null, api.lastWatchProviders)
        assertEquals(null, api.lastWatchRegion)
    }

    @Test
    fun `streaming services come back ordered by display priority`() = runTest(dispatcher) {
        val api = object : MovieApi by FakeApi(onList = { MovieListResponse() }) {
            override suspend fun watchProviders(watchRegion: String, language: String?) = WatchProviderListResponse(
                results = listOf(
                    ProviderDto(providerId = 2, providerName = "Later", displayPriority = 9, logoPath = "/b.jpg"),
                    ProviderDto(providerId = 1, providerName = "First", displayPriority = 1, logoPath = "/a.jpg"),
                ),
            )
        }
        val repo = repository(FakeMovieDao(), FakeGenreDao(), api)

        val result = repo.streamingServices()

        val services = assertIs<AppResult.Success<List<com.bigon.tmdb.model.WatchProvider>>>(result).value
        assertEquals(listOf("First", "Later"), services.map { it.name })
        assertEquals("https://image.tmdb.org/t/p/w92/a.jpg", services.first().logoUrl)
    }

    @Test
    fun `a film seen only in search results is still there for the detail screen`() = runTest(dispatcher) {
        // Search and discover results deliberately never reach movie_entity, so
        // without an in-memory fallback the detail screen opens with no artwork
        // at all — and the poster flying into it is a placeholder rather than
        // the image just tapped.
        val movieDao = FakeMovieDao()
        val api = FakeApi(onList = { MovieListResponse(results = listOf(movieDto(500, "Colony"))) })
        val repo = repository(movieDao, FakeGenreDao(listOf(GenreEntity(28, "Action"))), api)

        assertEquals(null, repo.observeCached(100L).first())

        val page = assertIs<AppResult.Success<com.bigon.tmdb.model.MoviePage>>(repo.search("spider"))
        val found = page.value.movies.first()

        val cached = repo.observeCached(found.id).first()
        assertEquals(found.title, cached?.title)
        // Still absent from the catalogue: this must not become a second one.
        assertTrue(movieDao.rows.value.values.flatten().none { it.id == found.id })
    }

    @Test
    fun `weekly and daily trending are cached under different keys`() = runTest(dispatcher) {
        // Sharing a key would make each refresh evict the other, which is the
        // whole reason categories are keyed rather than sharing a table.
        assertTrue(MovieCategory.Trending.listKey != MovieCategory.TrendingWeek.listKey)
    }
}
