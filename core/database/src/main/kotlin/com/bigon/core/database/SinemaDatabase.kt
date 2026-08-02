package com.bigon.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MovieEntity::class, GenreEntity::class, FavoriteEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class SinemaDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun genreDao(): GenreDao
    abstract fun favoriteDao(): FavoriteDao
}
