package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.ReviewPage
import javax.inject.Inject

/**
 * Loads one page of reviews. Separate from [GetMovieDetailUseCase] because the
 * request is separate and paginates on its own axis.
 */
class GetMovieReviewsUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Long, page: Int = 1): AppResult<ReviewPage> =
        repository.reviews(movieId, page)
}
