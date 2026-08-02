package com.bigon.sinema.ui.search

import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlag
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.MoviePage
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.domain.movie.DiscoverMoviesUseCase
import com.bigon.domain.movie.LoadMoreOutcome
import com.bigon.domain.movie.MovieRepository
import com.bigon.domain.movie.ObserveGenresUseCase
import com.bigon.domain.movie.SearchMoviesUseCase
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.MovieDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The debounce/cancellation contract: typing must not fire a request per
 * keystroke, and stale in-flight requests must never overwrite newer results.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeRepository : MovieRepository {
        var searchCalls = mutableListOf<String>()
        var discoverCalls = mutableListOf<Int?>()

        override fun observeCategory(category: MovieCategory): Flow<List<Movie>> = flowOf(emptyList())
        override suspend fun refresh(category: MovieCategory): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeCached(movieId: Long): Flow<Movie?> = flowOf(null)
        override suspend fun detail(movieId: Long): AppResult<MovieDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override fun observeGenres(): Flow<List<Genre>> = flowOf(listOf(Genre(878, "Science Fiction")))
        override suspend fun clearCatalogCache() = Unit

        var totalPages = 1
        override suspend fun search(query: String, page: Int): AppResult<MoviePage> {
            searchCalls += query
            return AppResult.Success(
                MoviePage(listOf(movie(page * 100L, "Result for $query p$page")), page, totalPages),
            )
        }

        override suspend fun discover(genreId: Int?, page: Int): AppResult<MoviePage> {
            discoverCalls += genreId
            return AppResult.Success(
                MoviePage(listOf(movie(page * 200L, "Discover p$page")), page, totalPages),
            )
        }

        override suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome> =
            AppResult.Success(LoadMoreOutcome.EndReached)

        private fun movie(id: Long, title: String) = Movie(
            id = id, title = title, overview = "", posterUrl = null,
            backdropUrl = null, releaseDate = null, voteAverage = null,
        )
    }

    private object NoopTracker : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
        override fun setUserId(id: String?) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
    }

    private class FixedFlags : FeatureFlagRepository {
        @Suppress("UNCHECKED_CAST")
        override fun <T> get(flag: FeatureFlag<T>): T = flag.default
        override fun <T> observe(flag: FeatureFlag<T>): Flow<T> = MutableStateFlow(flag.default)
        override suspend fun fetch(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun activate(): Boolean = false
    }

    private lateinit var repository: FakeRepository

    private fun viewModel(): SearchViewModel {
        repository = FakeRepository()
        return SearchViewModel(
            observeGenres = ObserveGenresUseCase(repository),
            searchMovies = SearchMoviesUseCase(repository),
            discoverMovies = DiscoverMoviesUseCase(repository),
            flags = FixedFlags(),
            tracker = NoopTracker,
        )
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `idle state browses via discover, not search`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf<Int?>(null), repository.discoverCalls)
        assertEquals(emptyList<String>(), repository.searchCalls)
        assertEquals(listOf("Discover p1"), vm.state.value.results.map { it.title })
    }

    @Test
    fun `typing debounces to a single request for the final query`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        // Simulate typing "dune" one keystroke at a time, faster than 300ms.
        listOf("d", "du", "dun", "dune").forEach {
            vm.onIntent(SearchIntent.QueryChanged(it))
            advanceTimeBy(50)
        }
        advanceUntilIdle()

        assertEquals(listOf("dune"), repository.searchCalls)
        assertEquals(listOf("Result for dune p1"), vm.state.value.results.map { it.title })
    }

    @Test
    fun `clearing the query returns to discover immediately`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()

        vm.onIntent(SearchIntent.QueryChanged(""))
        advanceUntilIdle()

        assertEquals(2, repository.discoverCalls.size) // initial + after clear
    }

    @Test
    fun `selecting a genre with a blank query hits discover with that genre`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(SearchIntent.GenreSelected(878))
        advanceUntilIdle()

        assertEquals(listOf(null, 878), repository.discoverCalls)
    }

    @Test
    fun `genre filters typed search results client-side`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()

        vm.onIntent(SearchIntent.GenreSelected(878))
        advanceUntilIdle()

        // The fake's search result carries no genres, so the filter removes it.
        assertNotNull(vm.state.value.selectedGenreName)
        assertEquals(emptyList<String>(), vm.state.value.visibleResults.map { it.title })
        // But the raw results are still there — clearing the chip restores them.
        assertEquals(1, vm.state.value.results.size)
    }

    // ── pagination ──────────────────────────────────────────────────────────

    @Test
    fun `loadMore appends the next page and advances the cursor`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.totalPages = 3
        advanceUntilIdle()
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.page)

        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.page)
        assertEquals(
            listOf("Result for dune p1", "Result for dune p2"),
            vm.state.value.results.map { it.title },
        )
    }

    @Test
    fun `loadMore is ignored on the final page`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.totalPages = 1
        advanceUntilIdle()
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()
        val callsBefore = repository.searchCalls.size

        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(callsBefore, repository.searchCalls.size)
        assertEquals(1, vm.state.value.page)
    }

    @Test
    fun `a new query resets paging back to page one`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.totalPages = 5
        advanceUntilIdle()
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()
        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.page)

        vm.onIntent(SearchIntent.QueryChanged("heat"))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.page)
        assertEquals(listOf("Result for heat p1"), vm.state.value.results.map { it.title })
    }

    @Test
    fun `blank query pages through discover instead of search`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.totalPages = 4
        advanceUntilIdle()

        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(2, repository.discoverCalls.size)
        assertEquals(emptyList<String>(), repository.searchCalls)
        assertEquals(listOf("Discover p1", "Discover p2"), vm.state.value.results.map { it.title })
    }
}
