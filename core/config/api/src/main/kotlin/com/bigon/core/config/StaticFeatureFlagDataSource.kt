package com.bigon.core.config

import com.bigon.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-backend data source: every read falls through to overrides/compile-time
 * defaults. Bound in :app until the Firebase Remote Config adapter lands.
 */
class StaticFeatureFlagDataSource : FeatureFlagDataSource {
    override suspend fun fetch(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun activate(): Boolean = false

    override fun getString(key: String): String? = null
    override fun getBoolean(key: String): Boolean? = null
    override fun getInt(key: String): Int? = null

    override val activations: Flow<Unit> = emptyFlow()
}
