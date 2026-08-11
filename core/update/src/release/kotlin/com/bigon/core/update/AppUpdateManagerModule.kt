package com.bigon.core.update

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The real thing. Its debug counterpart lives in `src/debug` and can be told to
 * pretend an update exists; keeping that in a variant source set means no
 * release code path can reach a test double, rather than relying on a runtime
 * flag being set correctly.
 *
 * Google's `FakeAppUpdateManager` class still lands in the release DEX — it
 * ships inside the `app-update` artifact and this project has R8 off — but
 * nothing here references it, so it is dead weight rather than a live switch.
 * Enabling R8 (already on the backlog) removes it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppUpdateManagerModule {

    @Provides
    @Singleton
    fun appUpdateManager(@ApplicationContext context: Context): AppUpdateManager =
        AppUpdateManagerFactory.create(context)
}
