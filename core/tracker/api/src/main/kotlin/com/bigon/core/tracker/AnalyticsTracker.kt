package com.bigon.core.tracker

/** The port consumed by features/ViewModels — never an SDK type (§6.2). */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun setUserId(id: String?)
    fun setUserProperty(key: String, value: String?)
}
