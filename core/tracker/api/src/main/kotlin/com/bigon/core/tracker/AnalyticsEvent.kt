package com.bigon.core.tracker

/**
 * Typed event catalog (§6.1): a sealed hierarchy, not stringly-typed calls —
 * enabling compile-time review, exhaustive audits, and a single naming convention.
 * Feature-specific events are added here as nested sealed interfaces
 * (e.g. `sealed interface Home : AnalyticsEvent`) when their feature lands.
 *
 * Naming convention: `name` is snake_case and unique across the catalog
 * (enforced by a Konsist test).
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any> get() = emptyMap()

    data class ScreenView(val screenName: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val params = mapOf("screen_name" to screenName)
    }

    /** Emitted when a screen actually renders an experiment variant (§7.4). */
    data class ExperimentExposed(val flagKey: String, val variant: String) : AnalyticsEvent {
        override val name = "experiment_exposed"
        override val params = mapOf("flag_key" to flagKey, "variant" to variant)
    }
}
