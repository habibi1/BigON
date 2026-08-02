package com.bigon.core.tracker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CompositeAnalyticsTrackerTest {

    private class RecordingSink(
        override val id: String,
        private val accept: (AnalyticsEvent) -> Boolean = { true },
    ) : AnalyticsSink {
        val events = mutableListOf<AnalyticsEvent>()
        override fun accepts(event: AnalyticsEvent) = accept(event)
        override fun track(event: AnalyticsEvent) { events += event }
        override fun setUserId(id: String?) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
    }

    private class MutableConsent(initial: Boolean) : ConsentProvider {
        val state = MutableStateFlow(initial)
        override val analyticsAllowed: StateFlow<Boolean> = state
    }

    @Test
    fun `fans out events to all accepting sinks`() = runTest(UnconfinedTestDispatcher()) {
        val sinkA = RecordingSink("a")
        val sinkB = RecordingSink("b", accept = { it.name != "screen_view" })
        val tracker = CompositeAnalyticsTracker(
            sinks = listOf(sinkA, sinkB),
            consent = MutableConsent(initial = true),
            scope = backgroundScope,
        )

        tracker.track(AnalyticsEvent.ScreenView("home"))
        tracker.track(AnalyticsEvent.ExperimentExposed("home_layout_variant", "control"))

        assertEquals(listOf("screen_view", "experiment_exposed"), sinkA.events.map { it.name })
        assertEquals(listOf("experiment_exposed"), sinkB.events.map { it.name })
    }

    @Test
    fun `drops events while consent is denied`() = runTest(UnconfinedTestDispatcher()) {
        val sink = RecordingSink("a")
        val consent = MutableConsent(initial = false)
        val tracker = CompositeAnalyticsTracker(
            sinks = listOf(sink),
            consent = consent,
            scope = backgroundScope,
        )

        tracker.track(AnalyticsEvent.ScreenView("home"))
        assertTrue(sink.events.isEmpty())

        consent.state.value = true
        tracker.track(AnalyticsEvent.ScreenView("detail"))
        assertEquals(listOf("screen_view"), sink.events.map { it.name })
    }

    @Test
    fun `event params carry their payload`() {
        val event = AnalyticsEvent.ExperimentExposed(flagKey = "k", variant = "v")
        assertEquals(mapOf("flag_key" to "k", "variant" to "v"), event.params)
    }
}
