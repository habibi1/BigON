package com.bigon.domain.movie

import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the cached titles of one category. */
class ObserveMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(category: MovieCategory): Flow<List<Movie>> =
        repository.observeCategory(category)
}
