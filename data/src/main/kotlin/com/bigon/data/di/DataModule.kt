package com.bigon.data.di

import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.config.FlagOverrides
import com.bigon.core.config.InMemoryFlagOverrides
import com.bigon.core.network.ApiCaller
import com.bigon.data.config.DefaultFeatureFlagRepository
import com.bigon.data.movie.DefaultFavoritesRepository
import com.bigon.data.movie.DefaultMovieRepository
import com.bigon.data.movie.MovieApi
import com.bigon.domain.movie.FavoritesRepository
import com.bigon.domain.movie.MovieRepository
import retrofit2.Retrofit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository implementations are bound here (§3.3 — features depend on :domain
 * and port APIs; implementations arrive via DI). Backend adapters
 * (FeatureFlagDataSource, analytics sinks) are wired in :app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: DefaultMovieRepository): MovieRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: DefaultFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepository(
        impl: DefaultFeatureFlagRepository,
    ): FeatureFlagRepository

    companion object {
        @Provides
        @Singleton
        fun provideFlagOverrides(): FlagOverrides = InMemoryFlagOverrides()

        /** Retrofit builds the service; the composition root supplies Retrofit. */
        @Provides
        @Singleton
        fun provideMovieApi(retrofit: Retrofit): MovieApi = retrofit.create(MovieApi::class.java)
    }
}
