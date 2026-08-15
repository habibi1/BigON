package com.bigon.tmdb.database

import androidx.room.TypeConverter

/** Stores genre id lists as a compact CSV column. */
class DatabaseConverters {

    @TypeConverter
    fun fromGenreIds(ids: List<Int>): String = ids.joinToString(separator = ",")

    @TypeConverter
    fun toGenreIds(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull(String::toIntOrNull)

    // Genre *names* (favourites snapshot). Names can contain spaces but not '|'.
    @TypeConverter
    fun fromGenreNames(names: List<String>): String = names.joinToString(separator = "|")

    @TypeConverter
    fun toGenreNames(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
