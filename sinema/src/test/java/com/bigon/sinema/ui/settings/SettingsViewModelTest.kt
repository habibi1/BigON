package com.bigon.sinema.ui.settings

import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.datastore.PreferenceStorage
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
import com.bigon.core.model.MovieCollection
import com.bigon.core.model.PersonDetail
import com.bigon.core.model.TvDetail
import com.bigon.domain.movie.DiscoverFilters
import com.bigon.core.model.Region
import com.bigon.core.model.ReviewPage
import com.bigon.core.model.TrendingItem
import com.bigon.core.model.WatchProvider
import com.bigon.domain.movie.LoadMoreOutcome
import com.bigon.domain.movie.MovieRepository
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.sinema.di.CacheOperator
import com.bigon.sinema.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakePreferences : PreferenceStorage {
        val theme = MutableStateFlow<String?>(null)
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setOnboardingCompleted(value: Boolean) = Unit
        override val themeMode: Flow<String?> get() = theme
        val regionFlow = MutableStateFlow<String?>(null)
        override val region: Flow<String?> = regionFlow
        override suspend fun setRegion(value: String?) { regionFlow.value = value }

        override suspend fun setThemeMode(value: String) { theme.value = value }
    }

    private class FakeCache : CacheOperator {
        var size = 1_048_576L * 128
        var cleared = 0
        override suspend fun sizeBytes(): Long = size
        override suspend fun clear() { cleared++; size = 0L }
    }


    /**
     * Only the region surface matters here; the rest of MovieRepository is
     * stubbed so this test exercises Settings and not the data layer.
     */
    private class FakeRepository(
        /**
         * The real repository writes the choice through to storage, and the
         * ViewModel learns about it from that flow. A fake that only records
         * the call would leave that wiring untested.
         */
        private val preferences: FakePreferences? = null,
    ) : MovieRepository {
        var regions: AppResult<List<Region>> = AppResult.Success(
            listOf(Region("ID", "Indonesia"), Region("US", "United States")),
        )
        var setRegionCalls = mutableListOf<String?>()
        var active = "US"

        override suspend fun availableRegions(): AppResult<List<Region>> = regions
        override suspend fun activeRegion(): String = active
        override suspend fun setRegion(code: String?) {
            setRegionCalls += code
            preferences?.regionFlow?.value = code
            active = code ?: active
        }

        override fun observeCategory(category: MovieCategory): Flow<List<Movie>> = flowOf(emptyList())
        override suspend fun refresh(category: MovieCategory): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome> =
            AppResult.Success(LoadMoreOutcome.EndReached)
        override fun observeCached(movieId: Long): Flow<Movie?> = flowOf(null)
        override suspend fun detail(movieId: Long): AppResult<MovieDetail> =
            AppResult.Failure(AppError.Unknown("unused"))
        override fun observeGenres(): Flow<List<Genre>> = flowOf(emptyList())
        override suspend fun search(query: String, page: Int): AppResult<MoviePage> =
            AppResult.Failure(AppError.Unknown("unused"))
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
        ): AppResult<MoviePage> = AppResult.Failure(AppError.Unknown("unused"))
        override suspend fun streamingServices(): AppResult<List<WatchProvider>> = AppResult.Success(emptyList())
        override fun observeTrendingAll(): Flow<List<TrendingItem>> = flowOf(emptyList())
        override suspend fun refreshTrendingAll(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun reviews(movieId: Long, page: Int): AppResult<ReviewPage> =
            AppResult.Success(ReviewPage(emptyList(), 1, 1, 0))
        override suspend fun clearCatalogCache() = Unit
    }

    private object NoopTracker : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
        override fun setUserId(id: String?) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `theme selection persists and reflects back into state`() = runTest(dispatcher) {
        val preferences = FakePreferences()
        val vm = SettingsViewModel(preferences, FakeCache(), FakeRepository(), NoopTracker)
        advanceUntilIdle()
        assertEquals(ThemeMode.System, vm.state.value.themeMode)

        vm.onIntent(SettingsIntent.ThemeChanged(ThemeMode.Dark))
        advanceUntilIdle()

        assertEquals("dark", preferences.theme.value)
        assertEquals(ThemeMode.Dark, vm.state.value.themeMode)
    }

    @Test
    fun `cache size is reported human-readably`() = runTest(dispatcher) {
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), FakeRepository(), NoopTracker)
        advanceUntilIdle()
        assertEquals("128 MB", vm.state.value.cacheLabel)
    }

    @Test
    fun `clearing cache invokes the operator and recomputes the size`() = runTest(dispatcher) {
        val cache = FakeCache()
        val vm = SettingsViewModel(FakePreferences(), cache, FakeRepository(), NoopTracker)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.ClearCache)
        advanceUntilIdle()

        assertEquals(1, cache.cleared)
        assertEquals("0 KB", vm.state.value.cacheLabel)
    }

    // ── region ──────────────────────────────────────────────────────────────

    @Test
    fun `the row distinguishes an inherited region from a chosen one`() = runTest(dispatcher) {
        val preferences = FakePreferences()
        val repo = FakeRepository(preferences).apply { active = "ID" }
        val vm = SettingsViewModel(preferences, FakeCache(), repo, NoopTracker)
        vm.onIntent(SettingsIntent.RegionPickerOpened)
        advanceUntilIdle()

        // Nothing chosen yet: the region came from the device and keeps
        // tracking it, which the label has to say.
        assertEquals("Indonesia · device", vm.state.value.regionLabel)

        vm.onIntent(SettingsIntent.RegionSelected("ID"))
        advanceUntilIdle()

        assertEquals("Indonesia", vm.state.value.regionLabel)
    }

    @Test
    fun `choosing a region persists it`() = runTest(dispatcher) {
        val preferences = FakePreferences()
        val repo = FakeRepository(preferences)
        val vm = SettingsViewModel(preferences, FakeCache(), repo, NoopTracker)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.RegionSelected("ID"))
        advanceUntilIdle()

        assertEquals(listOf<String?>("ID"), repo.setRegionCalls)
        assertEquals(false, vm.state.value.isRegionPickerOpen)
    }

    @Test
    fun `follow device is a null selection, not a resolved code`() = runTest(dispatcher) {
        // Storing the resolved code would freeze the region at whatever the
        // phone said today, defeating the point of the option.
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), FakeRepository(), NoopTracker)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.RegionSelected(null))
        advanceUntilIdle()

        assertEquals(null, vm.state.value.chosenRegion)
    }

    @Test
    fun `the picker searches by name and by code`() = runTest(dispatcher) {
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), FakeRepository(), NoopTracker)
        vm.onIntent(SettingsIntent.RegionPickerOpened)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.visibleRegions.size)

        vm.onIntent(SettingsIntent.RegionQueryChanged("indo"))
        assertEquals(listOf("Indonesia"), vm.state.value.visibleRegions.map { it.name })

        // Someone who knows the code should not have to recall the name.
        vm.onIntent(SettingsIntent.RegionQueryChanged("us"))
        assertEquals(listOf("United States"), vm.state.value.visibleRegions.map { it.name })
    }

    @Test
    fun `the region list is fetched once, when the picker first opens`() = runTest(dispatcher) {
        var calls = 0
        val repo = object : MovieRepository by FakeRepository() {
            override suspend fun availableRegions(): AppResult<List<Region>> {
                calls++
                return AppResult.Success(listOf(Region("ID", "Indonesia")))
            }
        }
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), repo, NoopTracker)
        advanceUntilIdle()
        assertEquals(0, calls) // not at startup — most sessions never open it

        vm.onIntent(SettingsIntent.RegionPickerOpened)
        advanceUntilIdle()
        vm.onIntent(SettingsIntent.RegionPickerDismissed)
        vm.onIntent(SettingsIntent.RegionPickerOpened)
        advanceUntilIdle()

        assertEquals(1, calls)
    }

    @Test
    fun `a failed region fetch leaves the rest of settings usable`() = runTest(dispatcher) {
        val repo = FakeRepository().apply { regions = AppResult.Failure(AppError.Network.NoConnection) }
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), repo, NoopTracker)
        vm.onIntent(SettingsIntent.RegionPickerOpened)
        advanceUntilIdle()

        assertEquals(true, vm.state.value.regionError != null)
        assertEquals("128 MB", vm.state.value.cacheLabel)
    }
}
