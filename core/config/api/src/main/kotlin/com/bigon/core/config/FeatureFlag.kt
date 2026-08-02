package com.bigon.core.config

/**
 * Typed flag definitions (§7.2). Every flag has a compile-time default, so the
 * app is fully functional offline / before first fetch.
 */
sealed class FeatureFlag<T>(val key: String, val default: T) {
    class BooleanFlag(key: String, default: Boolean) : FeatureFlag<Boolean>(key, default)
    class StringFlag(key: String, default: String) : FeatureFlag<String>(key, default)
    class IntFlag(key: String, default: Int) : FeatureFlag<Int>(key, default)
}

/** The reviewable, single-file flag registry. */
object Flags {
    val HomeLayoutVariant = FeatureFlag.StringFlag("home_layout_variant", default = "control")
    val SearchDebounceMs = FeatureFlag.IntFlag("search_debounce_ms", default = 300)

    val all: List<FeatureFlag<*>> = listOf(
        HomeLayoutVariant,
        SearchDebounceMs,
    )
}
