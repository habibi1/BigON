package com.bigon.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The mixed trending feed, cached so this list behaves like every other one in
 * the app rather than being the single chip that blanks without a connection.
 *
 * It cannot reuse [MovieEntity]: a series has no release date and a person has
 * neither rating nor overview, so the columns diverge. The primary key is
 * composite because ids are only unique *within* a media type — film 550 and
 * series 550 are different things, and a single-column key would silently
 * overwrite one with the other.
 */
@Entity(tableName = "trending_item_entity", primaryKeys = ["id", "media_type"])
data class TrendingItemEntity(
    val id: Long,
    @ColumnInfo(name = "media_type") val mediaType: String,
    /** Server ordering; the feed is ranked and that ranking is the content. */
    val position: Int,
    val title: String,
    val overview: String,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    /** Release date for a film, first-air date for a series, null for a person. */
    @ColumnInfo(name = "dated_on") val datedOn: String?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double?,
    @ColumnInfo(name = "genre_ids") val genreIds: List<Int>,
    @ColumnInfo(name = "known_for_department") val knownForDepartment: String?,
    @ColumnInfo(name = "known_for") val knownFor: List<String>,
)
