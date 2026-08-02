package com.bigon.core.config

import com.bigon.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** Port consumed by ViewModels/use cases (§7.2). */
interface FeatureFlagRepository {

    /** Sync, cache-backed, never suspends. */
    fun <T> get(flag: FeatureFlag<T>): T

    /** Reacts to activation (and debug overrides). */
    fun <T> observe(flag: FeatureFlag<T>): Flow<T>

    /** Background refresh — fetches without activating (§7.3 lifecycle). */
    suspend fun fetch(): AppResult<Unit>

    /** Atomically applies fetched values. */
    suspend fun activate(): Boolean
}
