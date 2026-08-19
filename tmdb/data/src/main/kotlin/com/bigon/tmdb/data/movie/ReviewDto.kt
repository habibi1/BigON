package com.bigon.tmdb.data.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/movie/{id}/reviews` — its own request rather than an appended block.
 *
 * Reviews have their own pagination and their own failure mode, and keeping
 * them off the detail payload means neither leaks into the other: a review
 * page can fail, retry, or run out without touching content that already
 * loaded.
 */
@Serializable
data class ReviewListResponse(
    val page: Int = 1,
    val results: List<ReviewDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class ReviewDto(
    val id: String = "",
    val author: String = "",
    @SerialName("author_details") val authorDetails: AuthorDetailsDto = AuthorDetailsDto(),
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AuthorDetailsDto(
    val username: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    /** TMDB's 0–10 scale, null when the reviewer left prose without a score. */
    val rating: Double? = null,
)
