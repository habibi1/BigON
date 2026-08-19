package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Movie
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<List<Movie>> = repository.observeFavorites()
}
