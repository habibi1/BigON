package com.bigon.tmdb.model

import java.time.LocalDate

/**
 * A TMDB user review. Loaded on demand rather than with the rest of detail —
 * see `MovieApi.reviews` for why.
 */
data class Review(
    val id: String,
    val author: String,
    val content: String,
    val avatarUrl: String?,
    /** TMDB's 0–10 author score; null when the reviewer wrote prose only. */
    val rating: Double?,
    val createdAt: LocalDate?,
)

/** One page of reviews, with enough context to know whether more exist. */
data class ReviewPage(
    val reviews: List<Review>,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}
