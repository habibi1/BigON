package com.bigon.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached detail payload, so opening a title a second time costs nothing and
 * works with no connection.
 *
 * The detail response is deeply nested — cast, recommendations, keywords and
 * per-region providers — which the CSV converters used elsewhere cannot carry.
 * Rather than normalise into five tables for data that is only ever read whole,
 * the trimmed result is stored as one JSON document. What goes in that column
 * is owned by :data; this module deliberately treats it as opaque text.
 *
 * [region] is stored because certification and streaming availability are
 * resolved per country. A row fetched for one region is still shown when the
 * device moves to another — stale beats blank — but the mismatch is visible to
 * the repository, which refreshes in the background.
 */
@Entity(tableName = "movie_detail_entity")
data class MovieDetailEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "region") val region: String,
    @ColumnInfo(name = "payload") val payload: String,
    /** Epoch millis, for eviction of the least recently refreshed rows. */
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
