package com.bigon.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── collections ─────────────────────────────────────────────────────────────

/**
 * `/collection/{id}`. Small — the Iron Man collection is 2 KB for three parts —
 * because `parts` reuses the list shape rather than embedding full details.
 */
@Serializable
data class CollectionResponse(
    val id: Long = 0,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val parts: List<MovieDto> = emptyList(),
)

/** The stub already present on a movie's detail payload. */
@Serializable
data class CollectionRefDto(
    val id: Long = 0,
    val name: String = "",
)
