package com.bigon.tmdb.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MovieEntity::class,
        GenreEntity::class,
        FavoriteEntity::class,
        MovieDetailEntity::class,
        TrendingItemEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class TmdbDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun genreDao(): GenreDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun trendingItemDao(): TrendingItemDao
}
