package com.bigon.sinema

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class SinemaApplication : Application(), SingletonImageLoader.Factory {

    /** Provider, not a direct injection: Coil may ask before the graph is warm. */
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
