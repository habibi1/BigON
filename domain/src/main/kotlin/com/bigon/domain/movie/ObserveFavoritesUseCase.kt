package com.bigon.domain.movie

import com.bigon.core.model.Movie
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<List<Movie>> = repository.observeFavorites()
}
