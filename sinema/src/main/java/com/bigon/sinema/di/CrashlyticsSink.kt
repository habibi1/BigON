package com.bigon.sinema.di

import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsSink
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics as a second sink, which is not the same thing as a second
 * analytics backend.
 *
 * Crash *reporting* needs nothing from this class: Crashlytics installs its own
 * uncaught-exception handler when the SDK initialises, and would catch every
 * crash whether or not this existed. What this adds is the trail leading up to
 * one — the last events before the stack trace, attached to the report.
 *
 * A crash on the detail screen is a puzzle; a crash on the detail screen after
 * `movie_opened source=recommendations` is a reproduction. Sending the same
 * events to both sinks costs nothing, because the [AnalyticsSink] fan-out
 * already exists and Crashlytics keeps breadcrumbs in memory until they are
 * needed.
 */
class CrashlyticsSink(
    private val crashlytics: FirebaseCrashlytics,
) : AnalyticsSink {

    override val id: String = "crashlytics"

    override fun track(event: AnalyticsEvent) {
        crashlytics.log(
            if (event.params.isEmpty()) {
                event.name
            } else {
                "${event.name} ${event.params.entries.joinToString(" ") { "${it.key}=${it.value}" }}"
            },
        )
    }

    /**
     * Sinema has no accounts, so nothing ever calls this with a real identity.
     * It is implemented rather than left blank so that the day something does,
     * it goes to the same place as the events it belongs with.
     */
    override fun setUserId(id: String?) {
        crashlytics.setUserId(id.orEmpty())
    }

    override fun setUserProperty(key: String, value: String?) {
        crashlytics.setCustomKey(key, value.orEmpty())
    }
}
