package com.bigon.sinema.ui.home

import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.tmdb.domain.movie.LoadMoreMoviesUseCase
import com.bigon.tmdb.domain.movie.LoadMoreOutcome
import com.bigon.tmdb.domain.movie.MovieRepository
import com.bigon.tmdb.domain.movie.ObserveMoviesUseCase
import com.bigon.tmdb.domain.movie.ObserveTrendingAllUseCase
import com.bigon.tmdb.domain.movie.RefreshMoviesUseCase
import com.bigon.tmdb.domain.movie.RefreshTrendingAllUseCase
import com.bigon.tmdb.model.Genre
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieCategory
import com.bigon.tmdb.model.MovieCollection
import com.bigon.tmdb.model.MovieDetail
import com.bigon.tmdb.model.MoviePage
import com.bigon.tmdb.model.PersonDetail
import com.bigon.tmdb.model.Region
import com.bigon.tmdb.model.ReviewPage
import com.bigon.tmdb.model.TrendingItem
import com.bigon.tmdb.model.TvDetail
import com.bigon.tmdb.model.WatchProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The catalogue is allowed to be emptied underneath this screen — changing the
 * region wipes every cached list, and so does Clear cache. Home has to notice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeRepository : MovieRepository {
        /**
         * A shared flow, not a state flow, because this fake has to be able to
         * report empty *twice*. A StateFlow conflates equal values, which
         * silently turns "the catalogue said empty three times" into one
         * emission — and a test written on top of that cannot tell a screen
         * that asks once from one that asks on every emission.
         */
        private val catalogue = MutableSharedFlow<List<Movie>>(replay = 1)
        var refreshCalls = 0
        var refreshResult: AppResult<Unit> = AppResult.Success(Unit)

        /** False for a feed TMDB genuinely has nothing for: refresh, still empty. */
        var repopulatesOnRefresh = true

        init { emit(listOf(movie(1, "Dune"))) }

        fun emit(movies: List<Movie>) {
            check(catalogue.tryEmit(movies)) { "buffer full" }
        }

        override fun observeCategory(category: MovieCategory): Flow<List<Movie>> = catalogue

        override suspend fun refresh(category: MovieCategory): AppResult<Unit> {
            refreshCalls++
            // A real refresh repopulates the table it just replaced.
            if (refreshResult is AppResult.Success && repopulatesOnRefresh) {
                emit(listOf(movie(1, "Dune")))
            }
            return refreshResult
        }

        override fun observeCached(movieId: Long): Flow<Movie?> = flowOf(null)
        override suspend fun detail(movieId: Long): AppResult<MovieDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override fun observeGenres(): Flow<List<Genre>> = flowOf(emptyList())
        override suspend fun clearCatalogCache() = Unit
        override fun observeTrendingAll(): Flow<List<TrendingItem>> = flowOf(emptyList())
        override suspend fun refreshTrendingAll(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun availableRegions(): AppResult<List<Region>> = AppResult.Success(emptyList())
        override suspend fun activeRegion(): String = "US"
        override suspend fun setRegion(code: String?) = Unit
        override suspend fun streamingServices(): AppResult<List<WatchProvider>> =
            AppResult.Success(emptyList())
        override suspend fun reviews(movieId: Long, page: Int): AppResult<ReviewPage> =
            AppResult.Success(ReviewPage(emptyList(), 1, 1, 0))
        override suspend fun search(query: String, page: Int): AppResult<MoviePage> =
            AppResult.Success(MoviePage(emptyList(), 1, 1))
        override suspend fun collection(collectionId: Long): AppResult<MovieCollection> =
            AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun person(personId: Long): AppResult<PersonDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun tvDetail(tvId: Long): AppResult<TvDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun discover(
            genreId: Int?,
            page: Int,
            streamingProviderId: Int?,
            filters: DiscoverFilters,
        ): AppResult<MoviePage> = AppResult.Success(MoviePage(emptyList(), 1, 1))
        override suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome> =
            AppResult.Success(LoadMoreOutcome.EndReached)

        companion object {
            fun movie(id: Long, title: String) = Movie(
                id = id, title = title, overview = "", posterUrl = null,
                backdropUrl = null, releaseDate = null, voteAverage = null,
            )
        }
    }

    private object NoopTracker : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
        override fun setUserId(id: String?) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
    }

    private fun viewModel(repo: MovieRepository) = HomeViewModel(
        ObserveMoviesUseCase(repo),
        RefreshMoviesUseCase(repo),
        ObserveTrendingAllUseCase(repo),
        RefreshTrendingAllUseCase(repo),
        LoadMoreMoviesUseCase(repo),
        NoopTracker,
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a catalogue emptied underneath the screen is refetched without being asked`() =
        runTest(dispatcher) {
            val repo = FakeRepository()
            val vm = viewModel(repo)
            advanceUntilIdle()

            assertEquals(1, repo.refreshCalls)
            assertEquals(1, vm.state.value.movies.size)

            // What changing the region does: every cached list is dropped,
            // because certification and availability are per-country.
            repo.emit(emptyList())
            advanceUntilIdle()

            // Without this the screen sat on its empty state until the reader
            // tapped a *different* chip — the same chip is scroll-to-top.
            assertEquals(2, repo.refreshCalls)
            assertEquals(1, vm.state.value.movies.size)
        }

    @Test
    fun `a feed that stays empty asks once, not forever`() = runTest(dispatcher) {
        val repo = FakeRepository()
        // The refresh works; TMDB simply has nothing for this feed. Every
        // emission that follows is empty, and every one of them satisfies the
        // condition that triggers a rescue — so the guard, not the condition,
        // is the only thing that can stop a request per emission.
        repo.repopulatesOnRefresh = false
        val vm = viewModel(repo)
        advanceUntilIdle()

        val afterStartup = repo.refreshCalls
        repeat(3) {
            repo.emit(emptyList())
            advanceUntilIdle()
        }

        assertEquals(afterStartup + 1, repo.refreshCalls)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a feed whose refresh failed shows the error rather than retrying itself`() =
        runTest(dispatcher) {
            val repo = FakeRepository()
            repo.refreshResult = AppResult.Failure(AppError.Network.NoConnection)
            val vm = viewModel(repo)
            advanceUntilIdle()

            val afterStartup = repo.refreshCalls
            repo.emit(emptyList())
            advanceUntilIdle()

            // The failure is on screen with a RETRY; rescuing it quietly would
            // hammer the network and never surface the error.
            assertEquals(afterStartup, repo.refreshCalls)
            assertNotNull(vm.state.value.error)
        }
}
