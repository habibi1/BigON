package com.bigon.core.tracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Portable fan-out dispatcher — the only [AnalyticsTracker] implementation (§6.2).
 * Events are queued and dispatched off the caller: [track] never blocks call sites.
 */
class CompositeAnalyticsTracker(
    private val sinks: List<AnalyticsSink>,
    private val consent: ConsentProvider,
    scope: CoroutineScope,
) : AnalyticsTracker {

    private val queue = Channel<AnalyticsEvent>(capacity = 256)

    init {
        queue.receiveAsFlow()
            .filter { consent.analyticsAllowed.value }
            .onEach { event ->
                sinks.filter { it.accepts(event) }.forEach { it.track(event) }
            }
            .launchIn(scope)
    }

    override fun track(event: AnalyticsEvent) {
        queue.trySend(event)
    }

    override fun setUserId(id: String?) = sinks.forEach { it.setUserId(id) }

    override fun setUserProperty(key: String, value: String?) =
        sinks.forEach { it.setUserProperty(key, value) }
}
