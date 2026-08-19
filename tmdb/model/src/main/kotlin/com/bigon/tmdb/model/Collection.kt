package com.bigon.tmdb.model

/**
 * A franchise as a single entity — the Iron Man collection returns three parts.
 *
 * Parts are ordinary [Movie]s: TMDB returns them in the same shape the list
 * endpoints use, so the existing card and detail route work unchanged and this
 * costs no new UI.
 */
data class MovieCollection(
    val id: Long,
    val name: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val parts: List<Movie>,
)

/**
 * The stub carried on a movie's own detail payload — enough to offer "part of
 * a collection" without a second request. The full parts list needs
 * `/collection/{id}`.
 */
data class CollectionRef(
    val id: Long,
    val name: String,
)
