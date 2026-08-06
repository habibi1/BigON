package com.bigon.data.movie

import com.bigon.core.common.AppResult
import com.bigon.core.common.DispatcherProvider
import com.bigon.core.database.GenreDao
import com.bigon.core.database.GenreEntity
import com.bigon.core.database.MovieDao
import com.bigon.core.database.MovieEntity
import com.bigon.core.network.ApiCaller
import com.bigon.core.network.error.NetworkErrorMapper
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Detail is the one screen that used to have no offline story. These are the
 * cases that make the difference visible: a second open, and an open with no
 * network at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailCacheTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class NoopMovieDao : MovieDao {
        override fun observeByList(listKey: String): Flow<List<MovieEntity>> = MutableStateFlow(emptyList())
        override fun observeById(id: Long): Flow<MovieEntity?> = MutableStateFlow(null)
        override suspend fun insertIgnoringDuplicates(movies: List<MovieEntity>) = Unit
        override suspend fun maxPage(listKey: String): Int? = null
        override suspend fun maxPosition(listKey: String): Int? = null
        override suspend fun deleteList(listKey: String) = Unit
        override suspend fun clearAll() = Unit
    }

    private class StubGenreDao(private val initial: List<GenreEntity>) : GenreDao {
        override fun observeAll(): Flow<List<GenreEntity>> = MutableStateFlow(initial)
        override suspend fun getAll(): List<GenreEntity> = initial
        override suspend fun count(): Int = initial.size
        override suspend fun upsertAll(genres: List<GenreEntity>) = Unit
    }

    private class DetailApi(
        private val response: (Long) -> MovieDetailResponse,
    ) : MovieApi {
        var calls = 0
        override suspend fun detail(id: Long, append: String, imageLanguage: String) =
            response(id).also { calls++ }
        override suspend fun reviews(id: Long, page: Int) = ReviewListResponse()
        override suspend fun trending(page: Int) = MovieListResponse()
        override suspend fun trendingWeek(page: Int) = MovieListResponse()
        override suspend fun popular(page: Int, region: String?) = MovieListResponse()
        override suspend fun nowPlaying(page: Int, region: String?) = MovieListResponse()
        override suspend fun topRated(page: Int, region: String?) = MovieListResponse()
        override suspend fun upcoming(page: Int, region: String?) = MovieListResponse()
        override suspend fun genres() = GenreListResponse()
        override suspend fun search(query: String, page: Int, includeAdult: Boolean) = MovieListResponse()
        override suspend fun discover(
            withGenres: String?,
            sortBy: String,
            page: Int,
            withWatchProviders: String?,
            watchRegion: String?,
        ) = MovieListResponse()
        override suspend fun watchProviders(watchRegion: String) = WatchProviderListResponse()
        override suspend fun trendingAll(page: Int) = TrendingListResponse()
        override suspend fun regions() = RegionListResponse()
        override suspend fun collection(id: Long) = CollectionResponse()
    }

    private fun dispatchers() = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = StandardTestDispatcher()
    }

    private fun repo(
        api: MovieApi,
        detailDao: DefaultMovieRepositoryTest.FakeMovieDetailDao,
        region: String = "US",
    ) = DefaultMovieRepository(
        NoopMovieDao(),
        StubGenreDao(listOf(GenreEntity(28, "Action"))),
        detailDao,
        DefaultMovieRepositoryTest.FakeTrendingItemDao(),
        api,
        ApiCaller(NetworkErrorMapper()),
        dispatchers(),
        object : RegionProvider {
            override suspend fun region() = region
            override fun language() = "en"
        },
        DefaultMovieRepositoryTest.FakePreferences(),
    )

    private fun richResponse(id: Long) = MovieDetailResponse(
        id = id,
        title = "Heat",
        tagline = "A crew",
        runtime = 170,
        voteAverage = 8.3,
        voteCount = 900,
        releaseDate = "1995-12-15",
        credits = CreditsDto(listOf(CastDto(id = 5, name = "Al Pacino", character = "Hanna", order = 0))),
        recommendations = MovieListResponse(results = listOf(MovieDto(id = 2, title = "Collateral"))),
        releaseDates = ReleaseDatesDto(
            listOf(ReleaseDateCountryDto("US", listOf(ReleaseDateDto("R", type = 3)))),
        ),
        keywords = KeywordsDto(listOf(KeywordDto(1, "heist"))),
        externalIds = ExternalIdsDto(imdbId = "tt0113277"),
        watchProviders = WatchProvidersDto(
            mapOf("US" to WatchProviderCountryDto(link = "https://tmdb", flatrate = listOf(ProviderDto(8, "Netflix")))),
        ),
        belongsToCollection = CollectionRefDto(id = 87359, name = "Mission: Impossible Collection"),
    )

    @Test
    fun `a successful detail read is cached`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao()
        val result = repo(DetailApi(::richResponse), dao).detail(1)

        assertIs<AppResult.Success<*>>(result)
        assertEquals(1, dao.rows.size)
        assertEquals("US", dao.rows.getValue(1L).region)
    }

    @Test
    fun `going offline falls back to the cached payload instead of failing`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao()
        var offline = false
        val api = DetailApi { id ->
            if (offline) throw IOException("offline") else richResponse(id)
        }

        repo(api, dao).detail(1)          // populates the cache
        offline = true
        val result = repo(api, dao).detail(1)

        val detail = assertIs<AppResult.Success<com.bigon.core.model.MovieDetail>>(result).value
        assertEquals("Heat", detail.title)
        assertEquals(listOf("Collateral"), detail.recommendations.map { it.title })
        assertEquals("R", detail.certification)
        assertEquals(listOf("heist"), detail.keywords)
        assertEquals("tt0113277", detail.imdbId)
        assertEquals(listOf("Netflix"), detail.watchProviders?.streaming?.map { it.name })
        assertEquals("Al Pacino", detail.cast.single().name)
    }

    @Test
    fun `an offline read with nothing cached still reports the failure`() = runTest(dispatcher) {
        val result = repo(
            DetailApi { throw IOException("offline") },
            DefaultMovieRepositoryTest.FakeMovieDetailDao(),
        ).detail(99)

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `a cache write failure never fails the read it was serving`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao().apply { failWrites = true }

        val result = repo(DetailApi(::richResponse), dao).detail(1)

        assertIs<AppResult.Success<*>>(result)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `a corrupt cached payload degrades to the network error, not a crash`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao()
        dao.rows[1L] = com.bigon.core.database.MovieDetailEntity(1, "US", "{not json", 0)

        val result = repo(DetailApi { throw IOException("offline") }, dao).detail(1)

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `clearing the catalogue cache clears cached details too`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao()
        val repository = repo(DetailApi(::richResponse), dao)
        repository.detail(1)
        assertEquals(1, dao.rows.size)

        repository.clearCatalogCache()

        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `the snapshot round trip preserves every appended block`() = runTest(dispatcher) {
        val dao = DefaultMovieRepositoryTest.FakeMovieDetailDao()
        val fresh = assertIs<AppResult.Success<com.bigon.core.model.MovieDetail>>(
            repo(DetailApi(::richResponse), dao).detail(1),
        ).value

        val fromCache = assertIs<AppResult.Success<com.bigon.core.model.MovieDetail>>(
            repo(DetailApi { throw IOException("offline") }, dao).detail(1),
        ).value

        // Equality across the whole aggregate: a field added to MovieDetail but
        // forgotten in the snapshot mapping fails here rather than silently
        // vanishing when the user goes offline.
        assertEquals(fresh, fromCache)
        assertNull(fresh.trailerKey)
        // Named explicitly because the franchise entry point is the one field
        // whose absence looks like the film simply has no collection.
        assertEquals(87359L, fromCache.collection?.id)
    }
}
