package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MoviePage
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: String, page: Int = 1): AppResult<MoviePage> =
        repository.search(query, page)
}
