package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MovieCategory
import javax.inject.Inject

/** Refreshes one category from the network; failure is returned, never thrown. */
class RefreshMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(category: MovieCategory): AppResult<Unit> =
        repository.refresh(category)
}
