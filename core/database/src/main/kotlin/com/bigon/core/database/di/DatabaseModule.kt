package com.bigon.core.database.di

import android.content.Context
import androidx.room.Room
import com.bigon.core.database.FavoriteDao
import com.bigon.core.database.GenreDao
import com.bigon.core.database.MovieDao
import com.bigon.core.database.SinemaDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): SinemaDatabase =
        Room.databaseBuilder(context, SinemaDatabase::class.java, "sinema.db")
            // Pre-release: the cache is disposable and can always be refetched,
            // so a schema change drops it rather than carrying migration code
            // that has never had real user data to migrate.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideMovieDao(database: SinemaDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideGenreDao(database: SinemaDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideFavoriteDao(database: SinemaDatabase): FavoriteDao = database.favoriteDao()
}
