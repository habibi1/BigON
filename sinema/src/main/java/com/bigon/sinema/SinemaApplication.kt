package com.bigon.sinema

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.bigon.sinema.di.ApplicationScope
import com.bigon.tmdb.domain.movie.RefreshStaleFavoritesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class SinemaApplication : Application(), SingletonImageLoader.Factory {

    /** Provider, not a direct injection: Coil may ask before the graph is warm. */
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    @Inject
    lateinit var refreshStaleFavorites: Provider<RefreshStaleFavoritesUseCase>

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // TMDB caps how long their content may be cached, and a favourite is the
        // one thing here that nothing else ever refetches. Launch is the only
        // moment the app is reliably running with a network but no user waiting
        // on it — and the pass is a no-op until a snapshot is six months old, so
        // in the overwhelming majority of launches it costs one indexed query.
        appScope.launch { refreshStaleFavorites.get().invoke() }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
