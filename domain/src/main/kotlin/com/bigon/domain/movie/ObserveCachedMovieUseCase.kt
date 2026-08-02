package com.bigon.domain.movie

import com.bigon.core.model.Movie
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Cached list data for one title — used to paint detail before the fetch lands. */
class ObserveCachedMovieUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(movieId: Long): Flow<Movie?> = repository.observeCached(movieId)
}
