package com.bigon.core.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Debug-only local overrides (§7.4): the repository consults overrides first,
 * so QA can switch variants without touching a backend console. A DataStore-backed
 * implementation (persistent across restarts) replaces the in-memory one when the
 * debug screen lands.
 */
interface FlagOverrides {
    fun <T> get(flag: FeatureFlag<T>): T?

    /** Emits whenever any override changes, so observers re-read. */
    val changes: Flow<Unit>
}

class InMemoryFlagOverrides : FlagOverrides {

    private val values = mutableMapOf<String, Any?>()
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override val changes: Flow<Unit> = _changes

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(flag: FeatureFlag<T>): T? = values[flag.key] as T?

    fun <T> set(flag: FeatureFlag<T>, value: T?) {
        if (value == null) values.remove(flag.key) else values[flag.key] = value
        _changes.tryEmit(Unit)
    }

    fun clear() {
        values.clear()
        _changes.tryEmit(Unit)
    }
}
