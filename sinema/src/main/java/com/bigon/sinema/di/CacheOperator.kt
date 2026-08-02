package com.bigon.sinema.di

import android.content.Context
import coil3.ImageLoader
import com.bigon.domain.movie.MovieRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The disposable storage Settings reports and clears: the Room catalogue cache
 * plus Coil's image caches. Favourites are deliberately outside its reach.
 * Interface-fronted so SettingsViewModel is testable without Android pieces.
 */
interface CacheOperator {
    suspend fun sizeBytes(): Long
    suspend fun clear()
}

@Singleton
class DefaultCacheOperator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: Provider<ImageLoader>,
    private val movieRepository: MovieRepository,
) : CacheOperator {

    override suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        val database = context.getDatabasePath("sinema.db")
        val imageCache = imageLoader.get().diskCache?.size ?: 0L
        (if (database.exists()) database.length() else 0L) + imageCache
    }

    override suspend fun clear() {
        movieRepository.clearCatalogCache()
        withContext(Dispatchers.IO) {
            imageLoader.get().apply {
                diskCache?.clear()
                memoryCache?.clear()
            }
        }
    }
}
