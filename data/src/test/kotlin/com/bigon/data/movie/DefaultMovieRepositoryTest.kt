package com.bigon.data.movie

import app.cash.turbine.test
import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.common.DispatcherProvider
import com.bigon.core.database.GenreDao
import com.bigon.core.database.GenreEntity
import com.bigon.core.database.MovieDao
import com.bigon.core.database.MovieEntity
import com.bigon.core.model.MovieCategory
import com.bigon.core.network.ApiCaller
import com.bigon.core.network.error.NetworkErrorMapper
import com.bigon.domain.movie.LoadMoreOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    ) : MovieApi {
        val requestedPages = mutableListOf<Int>()
        override suspend fun trending(page: Int) = onList().also { requestedPages += page }
        override suspend fun popular(page: Int) = onList().also { requestedPages += page }
        override suspend fun nowPlaying(page: Int) = onList().also { requestedPages += page }
        override suspend fun topRated(page: Int) = onList().also { requestedPages += page }
        override suspend fun upcoming(page: Int) = onList().also { requestedPages += page }
        override suspend fun genres() = onGenres()
        override suspend fun detail(id: Long, append: String) = MovieDetailResponse(id = id)
        var lastSearchQuery: String? = null
        var lastDiscoverGenres: String? = null
        var lastRequestedPage: Int? = null
        override suspend fun search(query: String, page: Int, includeAdult: Boolean): MovieListResponse {
            lastSearchQuery = query; lastRequestedPage = page; return onList()
        }
        override suspend fun discover(withGenres: String?, sortBy: String, page: Int): MovieListResponse {
            lastDiscoverGenres = withGenres; lastRequestedPage = page; return onList()
        }
    }

    private fun dispatchers() = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = StandardTestDispatcher()
    }

    private fun repository(
        movieDao: MovieDao,
        genreDao: GenreDao,
        api: MovieApi,
    ) = DefaultMovieRepository(movieDao, genreDao, api, ApiCaller(NetworkErrorMapper()), dispatchers())

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
}
