package com.bigon.core.tracker

/**
 * The SPI implemented by each SDK adapter (Firebase, Amplitude, …).
 * Adapters live in isolated non-portable modules; only :app sees them.
 */
interface AnalyticsSink {
    val id: String

    /** Per-sink filtering — lets a sink opt out of events it must not receive. */
    fun accepts(event: AnalyticsEvent): Boolean = true

    fun track(event: AnalyticsEvent)
    fun setUserId(id: String?)
    fun setUserProperty(key: String, value: String?)
}
