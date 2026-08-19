package com.bigon.tmdb.database.di

import android.content.Context
import androidx.room.Room
import com.bigon.tmdb.database.FavoriteDao
import com.bigon.tmdb.database.GenreDao
import com.bigon.tmdb.database.MovieDao
import com.bigon.tmdb.database.MovieDetailDao
import com.bigon.tmdb.database.TmdbDatabase
import com.bigon.tmdb.database.TrendingItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TmdbDatabase =
        Room.databaseBuilder(context, TmdbDatabase::class.java, "sinema.db")
            // Pre-release: the cache is disposable and can always be refetched,
            // so a schema change drops it rather than carrying migration code
            // that has never had real user data to migrate.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideMovieDao(database: TmdbDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideGenreDao(database: TmdbDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideFavoriteDao(database: TmdbDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideMovieDetailDao(database: TmdbDatabase): MovieDetailDao = database.movieDetailDao()

    @Provides
    fun provideTrendingItemDao(database: TmdbDatabase): TrendingItemDao = database.trendingItemDao()
}
