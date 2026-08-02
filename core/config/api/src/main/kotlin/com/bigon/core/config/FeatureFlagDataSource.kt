package com.bigon.core.config

import com.bigon.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * SPI implemented by backend adapters (§7.2): Firebase Remote Config today,
 * a custom flags endpoint later — swapping backends touches only :app wiring.
 * Getters return null when the backend has no (non-static) value for the key,
 * letting the repository fall through to overrides/defaults.
 */
interface FeatureFlagDataSource {
    suspend fun fetch(): AppResult<Unit>
    suspend fun activate(): Boolean

    fun getString(key: String): String?
    fun getBoolean(key: String): Boolean?
    fun getInt(key: String): Int?

    /** Emits every time a new config set is activated. */
    val activations: Flow<Unit>
}
