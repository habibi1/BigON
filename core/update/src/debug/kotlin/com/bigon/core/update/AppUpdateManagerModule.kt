package com.bigon.core.update

import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Debug counterpart of the release module.
 *
 * Play will not run an immediate update against a sideloaded build — it has no
 * release to compare against and answers UPDATE_NOT_AVAILABLE — so the flow
 * would otherwise be unverifiable until it was already in front of users.
 * Google ships [FakeAppUpdateManager] for exactly this, and it is the only way
 * to watch the gate behave before shipping it.
 *
 * Inert unless asked, and the knob belongs to this module rather than to
 * anything in the host app — so any app consuming :core:update can rehearse a
 * force update without writing a line of code:
 *
 *     ./gradlew installDebug -Pbigon.fakeUpdatePriority=5
 *     ./gradlew installDebug -Pbigon.fakeUpdatePriority=4 -Pbigon.fakeUpdateStalenessDays=7
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppUpdateManagerModule {

    private const val TAG = "ForceUpdate"

    @Provides
    @Singleton
    fun appUpdateManager(@ApplicationContext context: Context): AppUpdateManager {
        val priority = BuildConfig.FAKE_UPDATE_PRIORITY
        if (priority !in 0..5) return AppUpdateManagerFactory.create(context)

        Log.w(
            TAG,
            "Using FakeAppUpdateManager (priority=$priority, " +
                "staleness=${BuildConfig.FAKE_UPDATE_STALENESS_DAYS}d) — debug builds only",
        )
        return FakeAppUpdateManager(context).apply {
            // The fake never reaches Play, so it only needs a version above the
            // installed one; the exact number is immaterial.
            setUpdateAvailable(Int.MAX_VALUE)
            setUpdatePriority(priority)
            setClientVersionStalenessDays(BuildConfig.FAKE_UPDATE_STALENESS_DAYS)
        }
    }
}
