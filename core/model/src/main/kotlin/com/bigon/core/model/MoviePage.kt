package com.bigon.core.model

/** One page of a paginated result set, with enough metadata to ask for more. */
data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}
