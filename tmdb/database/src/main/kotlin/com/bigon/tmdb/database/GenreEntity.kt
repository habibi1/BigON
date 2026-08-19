package com.bigon.tmdb.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TMDB list endpoints return genre ids only, so the id→name table is fetched
 * once from `/genre/movie/list` and cached here. Keeping it local means genre
 * labels keep working offline.
 */
@Entity(tableName = "genre_entity")
data class GenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
