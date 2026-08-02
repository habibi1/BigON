package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MoviePage
import javax.inject.Inject

/** Genre-filtered browsing; null genre means most popular overall. */
class DiscoverMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(genreId: Int?, page: Int = 1): AppResult<MoviePage> =
        repository.discover(genreId, page)
}
