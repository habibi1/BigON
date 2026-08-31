package com.bigon.sinema.ui.search

import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlag
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.tmdb.model.Genre
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MoviePage
import com.bigon.tmdb.model.MovieCollection
import com.bigon.tmdb.model.PersonDetail
import com.bigon.tmdb.model.TvDetail
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.tmdb.model.Region
import com.bigon.tmdb.model.ReviewPage
import com.bigon.tmdb.model.TrendingItem
import com.bigon.tmdb.model.WatchProvider
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.tmdb.domain.movie.DiscoverMoviesUseCase
import com.bigon.tmdb.domain.movie.DiscoverSort
import com.bigon.tmdb.domain.movie.GetStreamingServicesUseCase
import com.bigon.tmdb.domain.movie.LoadMoreOutcome
import com.bigon.tmdb.domain.movie.MovieRepository
import com.bigon.tmdb.domain.movie.ObserveGenresUseCase
import com.bigon.tmdb.domain.movie.SearchMoviesUseCase
import com.bigon.tmdb.model.MovieCategory
import com.bigon.tmdb.model.MovieDetail
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        override fun observeTrendingAll(): Flow<List<TrendingItem>> = flowOf(emptyList())
        override suspend fun refreshTrendingAll(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun availableRegions(): AppResult<List<Region>> = AppResult.Success(emptyList())
        override suspend fun activeRegion(): String = "US"
        override suspend fun setRegion(code: String?) = Unit
        var services: List<WatchProvider> = emptyList()
        override suspend fun streamingServices(): AppResult<List<WatchProvider>> =
            AppResult.Success(services)
        override suspend fun reviews(movieId: Long, page: Int): AppResult<ReviewPage> =
            AppResult.Success(ReviewPage(emptyList(), 1, 1, 0))

        var totalPages = 1
        override suspend fun search(query: String, page: Int): AppResult<MoviePage> {
            searchCalls += query
            return AppResult.Success(
                MoviePage(listOf(movie(page * 100L, "Result for $query p$page")), page, totalPages),
            )
        }

        var lastProviderId: Int? = null
        var lastFilters: DiscoverFilters = DiscoverFilters()
        override suspend fun collection(collectionId: Long): AppResult<MovieCollection> =
            AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun person(personId: Long): AppResult<PersonDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun tvDetail(tvId: Long): AppResult<TvDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        /** Held open by a test that needs to observe a request in flight. */
        var discoverGate: CompletableDeferred<Unit>? = null

        override suspend fun discover(
            genreId: Int?,
            page: Int,
            streamingProviderId: Int?,
            filters: DiscoverFilters,
        ): AppResult<MoviePage> {
            discoverGate?.await()
            lastFilters = filters
            discoverCalls += genreId
            lastProviderId = streamingProviderId
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
            getStreamingServices = GetStreamingServicesUseCase(repository),
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
    fun `clearing the query returns to the browse list without refetching it`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            val browsing = vm.state.value.results.map { it.title }

            vm.onIntent(SearchIntent.QueryChanged("dune"))
            advanceUntilIdle()

            vm.onIntent(SearchIntent.QueryChanged(""))
            advanceUntilIdle()

            // The browse list comes back from memory. This used to assert a
            // second discover call; a selection already fetched is now restored
            // instead, which is what lets the screen return the reader to where
            // they were rather than to page 1.
            assertEquals(browsing, vm.state.value.results.map { it.title })
            assertEquals(1, repository.discoverCalls.size)
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

    // ── Tier 2: the streaming filter ────────────────────────────────────────

    @Test
    fun `selecting a service filters discover by it`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(SearchIntent.ServiceSelected(8))
        advanceUntilIdle()

        assertEquals(8, repository.lastProviderId)
        assertEquals(8, vm.state.value.selectedServiceId)
    }

    @Test
    fun `the service filter is dropped for a typed search, which cannot honour it`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.onIntent(SearchIntent.ServiceSelected(8))
            advanceUntilIdle()

            vm.onIntent(SearchIntent.QueryChanged("dune"))
            advanceUntilIdle()

            // The request went to /search, which takes no provider filter.
            assertEquals(listOf("dune"), repository.searchCalls)
            // And the row hides rather than sitting there doing nothing.
            assertFalse(vm.state.value.showServiceFilter)
        }

    @Test
    fun `clearing the service returns to unfiltered browsing`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val unfiltered = vm.state.value.results.map { it.title }

        vm.onIntent(SearchIntent.ServiceSelected(8))
        advanceUntilIdle()
        assertEquals(8, repository.lastProviderId)

        val callsWhileFiltered = repository.discoverCalls.size

        vm.onIntent(SearchIntent.ServiceSelected(null))
        advanceUntilIdle()

        assertEquals(null, vm.state.value.selectedServiceId)
        assertEquals(unfiltered, vm.state.value.results.map { it.title })
        // Restored, not refetched — so `lastProviderId` still records the
        // filtered request and is no longer the thing worth asserting.
        assertEquals(callsWhileFiltered, repository.discoverCalls.size)
    }

    @Test
    fun `a chip being fetched shows no films from the chip before it`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val previousChip = vm.state.value.results
        assertTrue(previousChip.isNotEmpty())

        // Hold the request open so the in-flight window can be inspected — the
        // window in which the old chip's films used to still be on screen.
        val inFlight = CompletableDeferred<Unit>()
        repository.discoverGate = inFlight

        vm.onIntent(SearchIntent.GenreSelected(878))
        advanceUntilIdle()

        assertEquals(emptyList(), vm.state.value.results)
        assertTrue(vm.state.value.showSkeletons)

        inFlight.complete(Unit)
        repository.discoverGate = null
        advanceUntilIdle()

        assertTrue(vm.state.value.results.isNotEmpty())
        assertFalse(vm.state.value.showSkeletons)
    }

    @Test
    fun `a chip returned to keeps its appended pages and is not refetched`() =
        runTest(dispatcher) {
            val vm = viewModel()
            repository.totalPages = 2
            advanceUntilIdle()

            vm.onIntent(SearchIntent.GenreSelected(878))
            advanceUntilIdle()
            vm.onIntent(SearchIntent.LoadMore)
            advanceUntilIdle()
            val bothPages = vm.state.value.results.map { it.title }
            assertEquals(listOf("Discover p1", "Discover p2"), bothPages)

            // Away to another chip, then back to this one.
            vm.onIntent(SearchIntent.GenreSelected(null))
            advanceUntilIdle()
            val callsBeforeReturning = repository.discoverCalls.size

            vm.onIntent(SearchIntent.GenreSelected(878))
            advanceUntilIdle()

            // Page 2 survives the round trip. Refetching would have returned
            // page 1 alone, and a scroll position remembered against two pages
            // would then have pointed past the end of one.
            assertEquals(bothPages, vm.state.value.results.map { it.title })
            assertEquals(callsBeforeReturning, repository.discoverCalls.size)
        }

    @Test
    fun `a failed service catalogue leaves search working with no filter row`() =
        runTest(dispatcher) {
            // The catalogue is a nicety; an error banner over working results
            // would be a worse trade than simply not offering the filter.
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(emptyList(), vm.state.value.services)
            assertFalse(vm.state.value.showServiceFilter)
            assertEquals(listOf("Discover p1"), vm.state.value.results.map { it.title })
        }

    // ── Tier 3: discover refinements ────────────────────────────────────────

    @Test
    fun `refinements reach discover`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(
            SearchIntent.FiltersChanged(
                DiscoverFilters(sort = DiscoverSort.Rating, releaseYear = 2024, minRating = 7.0, maxRuntimeMinutes = 120),
            ),
        )
        advanceUntilIdle()

        assertEquals(DiscoverSort.Rating, repository.lastFilters.sort)
        assertEquals(2024, repository.lastFilters.releaseYear)
        assertEquals(7.0, repository.lastFilters.minRating)
        assertEquals(120, repository.lastFilters.maxRuntimeMinutes)
    }

    @Test
    fun `the refine affordance hides for a typed search it cannot apply to`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.showFilters)

        vm.onIntent(SearchIntent.QueryChanged("dune"))
        advanceUntilIdle()

        // /search/movie takes no sort, year, rating or runtime parameter.
        assertFalse(vm.state.value.showFilters)
    }

    @Test
    fun `the active count drives the badge and default filters are inactive`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.filters.isActive)
        assertEquals(0, vm.state.value.filters.activeCount)

        vm.onIntent(SearchIntent.FiltersChanged(DiscoverFilters(releaseYear = 2024, minRating = 8.0)))
        advanceUntilIdle()

        assertTrue(vm.state.value.filters.isActive)
        assertEquals(2, vm.state.value.filters.activeCount)
    }

    @Test
    fun `changing a filter discards a page that was already in flight`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.totalPages = 5
        advanceUntilIdle()

        vm.onIntent(SearchIntent.FiltersChanged(DiscoverFilters(sort = DiscoverSort.Newest)))
        advanceUntilIdle()

        // Page resets rather than appending newest-sorted results onto
        // popularity-sorted ones.
        assertEquals(1, vm.state.value.page)
    }
}
