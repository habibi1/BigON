package com.bigon.sinema.di

import android.os.Bundle
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsSink
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * The Firebase Analytics adapter (§6.4).
 *
 * Lives in :app because it is the only module allowed to know Firebase exists —
 * the rest of the codebase talks to [AnalyticsSink], which is why adding this
 * changed no feature module.
 *
 * The event catalog is already snake_case and Firebase-shaped, so names pass
 * through untouched. Parameters are narrowed to the three types Firebase
 * accepts: anything else is written as its string form rather than dropped,
 * because a parameter silently missing from a dashboard is worse than one that
 * arrives in a slightly blunter shape.
 */
class FirebaseAnalyticsSink(
    private val analytics: FirebaseAnalytics,
) : AnalyticsSink {

    override val id: String = "firebase"

    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(event.name, event.params.toBundle())
    }

    override fun setUserId(id: String?) {
        analytics.setUserId(id)
    }

    override fun setUserProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value)
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle(size).also { bundle ->
        forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
    }
}
