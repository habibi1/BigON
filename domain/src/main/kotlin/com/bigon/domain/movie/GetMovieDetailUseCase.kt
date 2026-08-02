package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MovieDetail
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Long): AppResult<MovieDetail> = repository.detail(movieId)
}
