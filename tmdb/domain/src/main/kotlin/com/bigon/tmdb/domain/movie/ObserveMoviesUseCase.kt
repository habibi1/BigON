package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the cached titles of one category. */
class ObserveMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(category: MovieCategory): Flow<List<Movie>> =
        repository.observeCategory(category)
}
