package com.bigon.tmdb.data.config

import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlag
import com.bigon.core.config.FeatureFlagDataSource
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.config.FlagOverrides
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Composes: data-source value → local override (debug) → compile-time default
 * (§7.2), and emits on every activation so observe() consumers recompose.
 *
 * Note: overrides are consulted after the data source only in the sense of
 * §7.4 "the repository consults overrides first" — overrides win over backend
 * values, backend values win over defaults.
 */
class DefaultFeatureFlagRepository @Inject constructor(
    private val dataSource: FeatureFlagDataSource,
    private val overrides: FlagOverrides,
) : FeatureFlagRepository {

    override fun <T> get(flag: FeatureFlag<T>): T =
        overrides.get(flag)
            ?: dataSource.valueOf(flag)
            ?: flag.default

    override fun <T> observe(flag: FeatureFlag<T>): Flow<T> =
        merge(dataSource.activations, overrides.changes)
            .onStart { emit(Unit) }
            .map { get(flag) }
            .distinctUntilChanged()

    override suspend fun fetch(): AppResult<Unit> = dataSource.fetch()

    override suspend fun activate(): Boolean = dataSource.activate()

    @Suppress("UNCHECKED_CAST")
    private fun <T> FeatureFlagDataSource.valueOf(flag: FeatureFlag<T>): T? = when (flag) {
        is FeatureFlag.BooleanFlag -> getBoolean(flag.key)
        is FeatureFlag.StringFlag -> getString(flag.key)
        is FeatureFlag.IntFlag -> getInt(flag.key)
    } as T?
}
