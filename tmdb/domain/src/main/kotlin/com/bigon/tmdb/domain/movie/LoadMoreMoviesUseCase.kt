package com.bigon.tmdb.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.tmdb.model.MovieCategory
import javax.inject.Inject

/** Appends the next page of a category to the cache. */
class LoadMoreMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(category: MovieCategory): AppResult<LoadMoreOutcome> =
        repository.loadMore(category)
}
