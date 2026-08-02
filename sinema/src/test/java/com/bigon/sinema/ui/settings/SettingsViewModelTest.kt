package com.bigon.sinema.ui.settings

import com.bigon.core.datastore.PreferenceStorage
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.sinema.di.CacheOperator
import com.bigon.sinema.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        override suspend fun setThemeMode(value: String) { theme.value = value }
    }

    private class FakeCache : CacheOperator {
        var size = 1_048_576L * 128
        var cleared = 0
        override suspend fun sizeBytes(): Long = size
        override suspend fun clear() { cleared++; size = 0L }
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
        val vm = SettingsViewModel(preferences, FakeCache(), NoopTracker)
        advanceUntilIdle()
        assertEquals(ThemeMode.System, vm.state.value.themeMode)

        vm.onIntent(SettingsIntent.ThemeChanged(ThemeMode.Dark))
        advanceUntilIdle()

        assertEquals("dark", preferences.theme.value)
        assertEquals(ThemeMode.Dark, vm.state.value.themeMode)
    }

    @Test
    fun `cache size is reported human-readably`() = runTest(dispatcher) {
        val vm = SettingsViewModel(FakePreferences(), FakeCache(), NoopTracker)
        advanceUntilIdle()
        assertEquals("128 MB", vm.state.value.cacheLabel)
    }

    @Test
    fun `clearing cache invokes the operator and recomputes the size`() = runTest(dispatcher) {
        val cache = FakeCache()
        val vm = SettingsViewModel(FakePreferences(), cache, NoopTracker)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.ClearCache)
        advanceUntilIdle()

        assertEquals(1, cache.cleared)
        assertEquals("0 KB", vm.state.value.cacheLabel)
    }
}
