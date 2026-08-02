package com.bigon.sinema.di

import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsSink
import timber.log.Timber

/**
 * Debug sink (§6.4): lets QA inspect the event stream in logcat without any
 * SDK console. Replaced-alongside (not by) the Firebase sink when it lands.
 */
class TimberAnalyticsSink : AnalyticsSink {

    override val id: String = "timber"

    override fun track(event: AnalyticsEvent) {
        Timber.tag(TAG).d("track %s %s", event.name, event.params)
    }

    override fun setUserId(id: String?) {
        Timber.tag(TAG).d("setUserId %s", id)
    }

    override fun setUserProperty(key: String, value: String?) {
        Timber.tag(TAG).d("setUserProperty %s=%s", key, value)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
