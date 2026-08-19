package com.bigon.tmdb.data.config

import app.cash.turbine.test
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlag
import com.bigon.core.config.FeatureFlagDataSource
import com.bigon.core.config.InMemoryFlagOverrides
import com.bigon.core.config.StaticFeatureFlagDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class DefaultFeatureFlagRepositoryTest {

    private val flag = FeatureFlag.StringFlag("home_layout_variant", default = "control")

    private class FakeDataSource(
        private val strings: Map<String, String> = emptyMap(),
    ) : FeatureFlagDataSource {
        val activationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        override suspend fun fetch(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun activate(): Boolean {
            activationEvents.tryEmit(Unit)
            return true
        }
        override fun getString(key: String): String? = strings[key]
        override fun getBoolean(key: String): Boolean? = null
        override fun getInt(key: String): Int? = null
        override val activations: Flow<Unit> = activationEvents
    }

    @Test
    fun `falls back to compile-time default when backend has no value`() {
        val repository = DefaultFeatureFlagRepository(
            dataSource = StaticFeatureFlagDataSource(),
            overrides = InMemoryFlagOverrides(),
        )
        assertEquals("control", repository.get(flag))
    }

    @Test
    fun `backend value wins over default`() {
        val repository = DefaultFeatureFlagRepository(
            dataSource = FakeDataSource(strings = mapOf(flag.key to "variant_b")),
            overrides = InMemoryFlagOverrides(),
        )
        assertEquals("variant_b", repository.get(flag))
    }

    @Test
    fun `local override wins over backend value`() {
        val overrides = InMemoryFlagOverrides()
        overrides.set(flag, "forced")
        val repository = DefaultFeatureFlagRepository(
            dataSource = FakeDataSource(strings = mapOf(flag.key to "variant_b")),
            overrides = overrides,
        )
        assertEquals("forced", repository.get(flag))
    }

    @Test
    fun `observe emits current value and reacts to override changes`() = runTest {
        val overrides = InMemoryFlagOverrides()
        val repository = DefaultFeatureFlagRepository(
            dataSource = StaticFeatureFlagDataSource(),
            overrides = overrides,
        )

        repository.observe(flag).test {
            assertEquals("control", awaitItem())
            overrides.set(flag, "forced")
            assertEquals("forced", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
